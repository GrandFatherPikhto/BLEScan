package com.grandfatherpikhto.blin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BleScanManagerTest {

    private lateinit var closeable:AutoCloseable
    private val bleManager =
        BleManager(ApplicationProvider.getApplicationContext<Context?>().applicationContext,
            UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    /**
     * Проверяет состояние после запуска сканирования.
     * "Запускает" набор сгенерированных BluetoothDevices
     * и проверяет список отфильтрованных устройств на совпадение
     * Останавливает сканирование и проверяет состояние flowState
     */
    @Test
    fun testScan() = runTest(UnconfinedTestDispatcher()) {
        bleManager.startScan()
        assertEquals(BleScanManager.State.Scanning, bleManager.scanState)
        val scanResults = mockRandomScanResults(7)
        scanResults.forEach { scanResult ->
            bleManager.bleScanManager.onReceiveScanResult(scanResult)
        }
        assertEquals(bleManager.bleScanManager.scanResults.map { it.device }, scanResults.map { it.device })
        bleManager.stopScan()
        assertEquals(BleScanManager.State.Stopped, bleManager.scanState)
    }

    @Test
    fun testFilterScanNameWithStop() = runTest(UnconfinedTestDispatcher()) {
        val number = 2
        val scanResults = mockRandomScanResults(7, "Bluetooth%02d")
        val filterScanResult = scanResults[number]
        println("scanResults size: ${scanResults.size}")
        bleManager.startScan(names = listOf(filterScanResult.device.name),
            stopOnFind = true)
        assertEquals(BleScanManager.State.Scanning, bleManager.scanState)
        scanResults.forEach { scanResult ->
            bleManager.bleScanManager.onReceiveScanResult(scanResult)
        }

        assertEquals(listOf(filterScanResult.device),
            bleManager.bleScanManager.scanResults.map { it.device })
        assertEquals(BleScanManager.State.Stopped, bleManager.scanState)
    }

    @Test
    fun testFilterScanAddressWithStop() = runTest(UnconfinedTestDispatcher()) {
        val number = 2
        val scanResults = mockRandomScanResults(7, "Bluetooth%02d")
        val filterScanResult = scanResults[number]
        bleManager.startScan(addresses = listOf(filterScanResult.device.address),
            stopOnFind = true)
        assertEquals(BleScanManager.State.Scanning, bleManager.scanState)
        scanResults.forEach { scanResult ->
            bleManager.bleScanManager.onReceiveScanResult(scanResult)
        }
        assertEquals(listOf(filterScanResult.device),
            bleManager.bleScanManager.scanResults.map { it.device })
        assertEquals(BleScanManager.State.Stopped, bleManager.scanState)
    }
}