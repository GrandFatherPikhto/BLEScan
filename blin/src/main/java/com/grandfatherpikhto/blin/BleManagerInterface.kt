package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.ScanResult
import com.grandfatherpikhto.blin.buffer.BleCharacteristicNotify
import com.grandfatherpikhto.blin.data.BleGattItem
import com.grandfatherpikhto.blin.data.BleBondState
import com.grandfatherpikhto.blin.orig.AbstractBleBondManager
import com.grandfatherpikhto.blin.orig.AbstractBleGattManager
import com.grandfatherpikhto.blin.orig.AbstractBleScanManager
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BleManagerInterface {
    val bleScanManager: AbstractBleScanManager
    val bleGattManager: AbstractBleGattManager
    val bleBondManager: AbstractBleBondManager

    val stateFlowScanState:StateFlow<AbstractBleScanManager.State>
    val scanState: AbstractBleScanManager.State

    val sharedFlowScanResults: SharedFlow<ScanResult>
    val scanResults:List<ScanResult>

    val stateFlowScanError: StateFlow<Int>
    val scanError: Int

    val stateFlowConnectState: StateFlow<AbstractBleGattManager.State>
    val connectState: AbstractBleGattManager.State

    val sharedFlowConnectStateCode: SharedFlow<Int>

    val stateFlowBluetoothGatt: StateFlow<BluetoothGatt?>
    val bluetoothGatt: BluetoothGatt?

    val sharedFlowCharacteristic: SharedFlow<BluetoothGattCharacteristic>
    val sharedFlowDescriptor: SharedFlow<BluetoothGattDescriptor>

    val stateFlowBondState: StateFlow<BleBondState?>
    val bondState: BleBondState?

    val sharedFlowCharacteristicNotify: SharedFlow<BleCharacteristicNotify>
    val notifiedCharacteristic:List<BluetoothGattCharacteristic>

    fun onDestroy()

    fun bondRequest(address: String): Boolean

    fun startScan(addresses: List<String> = listOf(),
                  names: List<String> = listOf(),
                  services: List<String> = listOf(),
                  stopOnFind: Boolean = false,
                  filterRepeatable: Boolean = true,
                  stopTimeout: Long = 0L
    ) : Boolean

    fun stopScan()

    fun connect(address: String) : BluetoothGatt?
    fun disconnect()

    fun addGattData(bleGattData: BleGattItem)

    fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) : Boolean
    fun notifyCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic)
    fun notifyCharacteristic(bleGattData: BleGattItem)
    fun isCharacteristicNotified(bluetoothGattCharacteristic: BluetoothGattCharacteristic) : Boolean
}