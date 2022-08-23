package com.grandfatherpikhto.blin.buffer

import android.bluetooth.*
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.grandfatherpikhto.blin.data.BleGattItem
import com.grandfatherpikhto.blin.shadows.ShadowBluetoothGatt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows= [ ShadowBluetoothGatt::class ])
class QueueBufferTest {
    companion object {
        const val NAME="BLE_DEVICE"
    }
    private val applicationContext = ApplicationProvider.getApplicationContext<Context?>().applicationContext
    private val bluetoothAdapter = (applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val shadowBluetoothAdapter = shadowOf(bluetoothAdapter)
    private val bluetoothDevice = bluetoothAdapter.getRemoteDevice(randomBleAddress)
    private val shadowBluetoothDevice = shadowOf(bluetoothDevice)
    private val bluetoothGatt = ShadowBluetoothGatt.newInstance(bluetoothDevice)

    private val randomBleAddress get() = Random.nextBytes(6).joinToString (":"){ String.format("%02X", it) }

    @Before
    fun setUp() {
        shadowBluetoothDevice.setName(NAME)
        shadowBluetoothDevice.setBondState(BluetoothDevice.BOND_BONDED)
        // shadowBluetoothAdapter.bondedDevices.add(bluetoothDevice)
    }

    @After
    fun tearDown() {
    }

    private val dispatcher = UnconfinedTestDispatcher()

    private val queueBuffer = QueueBuffer(dispatcher)

    @Test
    fun testBluetoothGatt() {
        val service = bluetoothGatt.getService(bluetoothGatt.services[0].uuid)
        val characteristic = service.getCharacteristic(service.characteristics[0].uuid)

        assertNotNull(service)
        assertNotNull(characteristic)
    }

    @Test
    fun testQueue() = runTest (dispatcher) {
        queueBuffer.bluetoothGatt = bluetoothGatt
        val gattItems = mutableListOf<BleGattItem>()

        (0..99).forEach { idx ->
            val service = bluetoothGatt.services[idx.rem(bluetoothGatt.services.size)]
            val characteristic = service.characteristics[idx.rem(service.characteristics.size)]
            characteristic.value = Random.nextBytes(Random.nextInt(1, 10))
            val gattItem = BleGattItem(characteristic, BleGattItem.Type.Write)
            gattItems.add(gattItem)
            queueBuffer.addGattData(gattItem)
        }
        assertEquals(100, queueBuffer.count)

        gattItems.forEach { item ->
            queueBuffer.onCharacteristicWrite(bluetoothGatt, item.getCharacteristic(bluetoothGatt), BluetoothGatt.GATT_SUCCESS)
        }

        assertEquals(0, queueBuffer.count)
    }
}