package com.grandfatherpikhto.blin.helper

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.content.Intent
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RuntimeEnvironment
import kotlin.random.Random

fun mockBluetoothDevice(name: String? = null, address: String? = null): BluetoothDevice {
    val bluetoothDevice = mock<BluetoothDevice> { bluetoothDevice ->
        on {bluetoothDevice.name} doReturn name
        on {bluetoothDevice.address} doReturn (
            address ?: Random.nextBytes(6)
                .joinToString (":") {
                    String.format("%02X", it) })
        }

    return bluetoothDevice
}

fun mockScanResult(bluetoothDevice: BluetoothDevice) : ScanResult = ScanResult(
    bluetoothDevice,
    0, 0, 0, 0, 0, 0, 0,
    mock<ScanRecord>(),
    System.currentTimeMillis())

fun mockScanResultIntent(scanResults: List<ScanResult>) : Intent
        = Intent(RuntimeEnvironment.getApplication().applicationContext, ScanResult::class.java)
    .putParcelableArrayListExtra(
        BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
        scanResults.toCollection(ArrayList()))

fun mockRandomScanResults(num: Int, name: String? = null) : List<ScanResult> {
    val scanResults = mutableListOf<ScanResult>()
    (1..num).forEach { number ->
        val scanResult = mockScanResult(
            mockBluetoothDevice(name = String.format(name ?: "BLE_%02d", number))
        )
        scanResults.add(scanResult)
    }

    return scanResults.toList()
}

fun mockRandomIntentScanResults(num: Int, name: String? = null) : Intent =
    Intent(RuntimeEnvironment.getApplication().applicationContext, ScanResult::class.java)
        .putParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
            mockRandomScanResults(num, name).toCollection(ArrayList()))

fun Intent.toBluetoothDevices() : List<BluetoothDevice>
        = this.getParcelableArrayListExtra<ScanResult>(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
    ?.let { arrayList: java.util.ArrayList<ScanResult> ->
        arrayList.map { scanResult -> scanResult.device }
    } ?: listOf()

fun mockBluetoothGatt(address: String? = null, name: String? = null) : BluetoothGatt {
    val bluetoothDevice = mockBluetoothDevice(name, address)
    return mockBluetoothGatt(bluetoothDevice)
}

fun mockBluetoothGatt(bluetoothDevice: BluetoothDevice) : BluetoothGatt {
    val bluetoothGatt = mock<BluetoothGatt> { bluetoothGatt ->
        on { bluetoothGatt.discoverServices()} doReturn true
        on { bluetoothGatt.device } doReturn bluetoothDevice
    }
    return bluetoothGatt
}
