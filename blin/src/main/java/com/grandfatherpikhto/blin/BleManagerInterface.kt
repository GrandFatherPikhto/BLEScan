package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import androidx.lifecycle.DefaultLifecycleObserver
import com.grandfatherpikhto.blin.buffer.BleCharacteristicNotify
import com.grandfatherpikhto.blin.data.BleGattItem
import com.grandfatherpikhto.blin.data.BleBondState
import com.grandfatherpikhto.blin.data.BleGatt
import com.grandfatherpikhto.blin.data.BleScanResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BleManagerInterface : DefaultLifecycleObserver {
    val stateFlowScanState:StateFlow<BleScanManager.State>
    val scanState:BleScanManager.State

    val sharedFlowBleScanResult: SharedFlow<BleScanResult>
    val scanResults:List<BleScanResult>

    val stateFlowScanError: StateFlow<Int>
    val scanError: Int

    val stateFlowConnectState: StateFlow<BleGattManager.State>
    val connectState: BleGattManager.State

    val sharedFlowConnectStateCode: SharedFlow<Int>

    val stateFlowBleGatt: StateFlow<BleGatt?>
    val bleGatt: BleGatt?

    val sharedFlowCharacteristic: SharedFlow<BluetoothGattCharacteristic>
    val sharedFlowDescriptor: SharedFlow<BluetoothGattDescriptor>

    val stateFlowBondState: StateFlow<BleBondState?>
    val bondState: BleBondState?

    val sharedFlowCharacteristicNotify: SharedFlow<BleCharacteristicNotify>
    val notifiedCharacteristic:List<BluetoothGattCharacteristic>

    fun bondRequest(address: String): Boolean

    fun startScan(addresses: List<String> = listOf(),
                  names: List<String> = listOf(),
                  services: List<String> = listOf(),
                  stopOnFind: Boolean = false,
                  filterRepeatable: Boolean = true,
                  stopTimeout: Long = 0L
    ) : Boolean

    fun stopScan()

    fun connect(address: String) : BleGatt?
    fun disconnect()

    fun writeGattData(bleGattData: BleGattItem)

    fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) : Boolean
    fun readDescriptor(bluetoothGattDescriptor: BluetoothGattDescriptor) : Boolean
    fun readGattData(bleGattData: BleGattItem): Boolean
    fun notifyCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic)
    fun notifyCharacteristic(bleGattData: BleGattItem)
    fun isCharacteristicNotified(bluetoothGattCharacteristic: BluetoothGattCharacteristic) : Boolean
}