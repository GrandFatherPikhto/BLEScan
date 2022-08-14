package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import com.grandfatherpikhto.blin.helper.mockBluetoothDevice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.lenient
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BleBondManagerTest {
    companion object {
        const val ADDRESS = "01:02:03:04:05"
        const val NAME    = "BLE_DEVICE"
    }

    private lateinit var closeable:AutoCloseable
    private val bleManager =
        BleManager(
            ApplicationProvider.getApplicationContext<Context?>().applicationContext,
            UnconfinedTestDispatcher())

    private val bluetoothManager =
        (ApplicationProvider.getApplicationContext<Context?>()
            .getSystemService<BluetoothManager>())!!
    private val bluetoothAdapter = bluetoothManager.adapter

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    @Test
    fun bondDevice() = runTest(UnconfinedTestDispatcher()) {
        // Буквы адреса должны быть в ВЕРХНЕМ регистре
        val address = Random.nextBytes(6)
            .joinToString(":") { String.format("%02X", it)}
        val bluetoothDevice = mockBluetoothDevice(address = address, name = "BLE_DEVICE")
        lenient().`when`(bluetoothDevice.bondState).thenReturn(BluetoothDevice.BOND_NONE)
        lenient().`when`(bluetoothDevice.createBond()).thenReturn(true)
        assertEquals(BleBondManager.State.None, bleManager.bondState?.state)
        // bleManager.bondRequest(address)
        bleManager.bleBondManager.bondRequest(bluetoothDevice)
        assertEquals(BleBondManager.State.Bonding, bleManager.bondState?.state)
        lenient().`when`(bluetoothDevice.bondState).thenReturn(BluetoothDevice.BOND_BONDED)
        bleManager.bleBondManager.onSetBondingDevice(bluetoothDevice, BluetoothDevice.BOND_NONE, BluetoothDevice.BOND_BONDED)
        assertEquals(BleBondManager.State.Bonded, bleManager.bondState?.state)
    }

    @Test
    fun errorBondingDevice() = runTest(UnconfinedTestDispatcher()) {
        val bluetoothDevice = mockBluetoothDevice(address = ADDRESS, name = NAME)
        lenient().`when`(bluetoothDevice.bondState).thenReturn(BluetoothDevice.BOND_NONE)
        lenient().`when`(bluetoothDevice.createBond()).thenReturn(false)
        assertEquals(BleBondManager.State.None, bleManager.bondState?.state)
        bleManager.bleBondManager.bondRequest(bluetoothDevice)
        assertEquals(BleBondManager.State.Error, bleManager.bondState?.state)
    }

    @Test
    fun rejectBondDevice() = runTest(UnconfinedTestDispatcher()) {
        val bluetoothDevice = mockBluetoothDevice(address = ADDRESS, name = NAME)
        lenient().`when`(bluetoothDevice.bondState).thenReturn(BluetoothDevice.BOND_NONE)
        lenient().`when`(bluetoothDevice.createBond()).thenReturn(true)
        assertEquals(BleBondManager.State.None, bleManager.bondState?.state)
        bleManager.bleBondManager.bondRequest(bluetoothDevice)
        assertEquals(BleBondManager.State.Bonding, bleManager.bondState?.state)
        lenient().`when`(bluetoothDevice.bondState).thenReturn(BluetoothDevice.BOND_BONDED)
        bleManager.bleBondManager.onSetBondingDevice(bluetoothDevice, BluetoothDevice.BOND_NONE, BluetoothDevice.BOND_NONE)
        assertEquals(BleBondManager.State.Reject, bleManager.bondState?.state)
    }
}