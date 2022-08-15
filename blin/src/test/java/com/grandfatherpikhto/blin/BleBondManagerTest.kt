package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.grandfatherpikhto.blin.data.BleBondState
import com.grandfatherpikhto.blin.data.BleDevice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.*
import org.robolectric.Shadows.shadowOf import org.robolectric.shadows.*
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BleBondManagerTest {
    companion object {
        const val NAME = "BLE_DEVICE"
    }

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var closeable:AutoCloseable

    private lateinit var context: Context

    private val applicationContext = ApplicationProvider.getApplicationContext<Context>()
    private val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val shadowBluetoothManager = shadowOf(bluetoothManager)
    private val bluetoothAdapter = bluetoothManager.adapter
    private val shadowAdapter = shadowOf(bluetoothAdapter)

    private val bleBondManager = BleBondManager(applicationContext, dispatcher)

    private val controllerActivity = Robolectric.buildActivity(AppCompatActivity::class.java)
        .create()
        .start()

    private val appCompatActivity = controllerActivity.get()


    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        appCompatActivity.lifecycle.addObserver(bleBondManager)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    private fun putIntentDevice(bluetoothDevice: BluetoothDevice, newBondState: Int = BluetoothDevice.BOND_BONDED) =
        applicationContext.sendBroadcast(Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED).let {
            it.putExtra(BluetoothDevice.EXTRA_DEVICE, bluetoothDevice)
            it.putExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE)
            it.putExtra(BluetoothDevice.EXTRA_BOND_STATE, newBondState)
            it
        })

    private fun shadowBluetoothDevice(address: String, name: String, bondState: Int = BluetoothDevice.BOND_NONE) : BluetoothDevice =
        bluetoothAdapter.getRemoteDevice(address).let { bluetoothDevice ->
            shadowOf(bluetoothDevice).setBondState(bondState)
            shadowOf(bluetoothDevice).setName(name)
            return bluetoothDevice
        }

    private val randomBluetoothAddress: String
        get() = Random.nextBytes(6)
            .joinToString (":"){ String.format("%02X", it) }

    @Test
    fun bondDevice() = runTest(dispatcher) {
        // Буквы адреса должны быть в ВЕРХНЕМ регистре
        val address = randomBluetoothAddress
        val bluetoothDevice = shadowBluetoothDevice(address, NAME)
        shadowOf(bluetoothDevice).setCreatedBond(true)
        bleBondManager.bondRequest(address)
        assertEquals(BleBondState(BleDevice(bluetoothDevice),
            BleBondManager.State.Request), bleBondManager.bondState)
        shadowAdapter.setBondedDevices(mutableSetOf(bluetoothDevice))
        shadowOf(bluetoothDevice).setBondState(BluetoothDevice.BOND_BONDED)
        putIntentDevice(bluetoothDevice)
        ShadowLooper.shadowMainLooper().idle()
        assertEquals(BleBondState(BleDevice(bluetoothDevice), BleBondManager.State.Bonded),
            bleBondManager.bondState)
    }


    @Test
    fun errorBondingDevice() = runTest(UnconfinedTestDispatcher()) {
        val address = randomBluetoothAddress
        val bluetoothDevice = shadowBluetoothDevice(address, NAME)
        shadowBluetoothManager.addDevice(0, 0, bluetoothDevice)
        shadowOf(bluetoothDevice).setCreatedBond(false)
        bleBondManager.bondRequest(address)
        ShadowLooper.shadowMainLooper().idle()
        assertEquals(BleBondState(BleDevice(bluetoothDevice),
            BleBondManager.State.Error), bleBondManager.bondState)
    }

    @Test
    fun rejectBondDevice() = runTest(UnconfinedTestDispatcher()) {
        val address = randomBluetoothAddress
        val bluetoothDevice = shadowBluetoothDevice(address, NAME)
        shadowOf(bluetoothDevice).setCreatedBond(true)
        bleBondManager.bondRequest(address)
        assertEquals(BleBondState(BleDevice(bluetoothDevice),
            BleBondManager.State.Request), bleBondManager.bondState)
        shadowAdapter.setBondedDevices(mutableSetOf(bluetoothDevice))
        shadowOf(bluetoothDevice).setBondState(BluetoothDevice.BOND_NONE)
        putIntentDevice(bluetoothDevice, BluetoothDevice.BOND_NONE)
        ShadowLooper.shadowMainLooper().idle()
        assertEquals(BleBondState(BleDevice(bluetoothDevice), BleBondManager.State.Reject),
            bleBondManager.bondState)
    }
}