package com.grandfatherpikhto.blin

import android.bluetooth.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
interface BluetoothListener {
    fun onSetBluetoothAdapter(bluetoothAdapter: BluetoothAdapter?) {}
    fun onBluetoothPairingRequest(oldDevice: BluetoothDevice?, newDevice: BluetoothDevice?) {}
    fun onChangeBluetoothState(bluetoothDevice: BluetoothDevice?, oldState: Int, newState: Int) {}
    fun onChangeBluetoothBondState(bluetoothDevice: BluetoothDevice?, oldState: Int, newState: Int) {}
    fun onSetBluetoothDevice(oldValue: BluetoothDevice?, newValue: BluetoothDevice?) {}
    fun onSetCurrentDevice(oldValue: BluetoothDevice?, newValue: BluetoothDevice?) {}
    fun onBtLeInterfaceBound(oldValue: BtLeInterface?, newValue: BtLeInterface?) {}
    fun onBluetoothEnabled(enabled:Boolean) {}
    fun onBluetoothPaired(btLeDevice: BluetoothDevice?) {}
    fun onChangeScannerState(oldState: BtLeScanner.State, newState: BtLeScanner.State) {}
    fun onFindDevice(btLeDevice: BluetoothDevice?) {}
    fun onScanError(oldError: Int, newError:Int) {}
    fun onChangeConnectorState(oldState: BtLeConnector.State, newState: BtLeConnector.State) {}
    fun onGattError(oldError:Int, newError:Int) {}
    fun onCharacteristicWrite(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?, state: Int) {}
    fun onCharacteristicReaded(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?, state: Int) {}
    fun onCharacteristicChanged(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?) {}
    fun onDescriptorWrite(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state: Int) {}
    fun onDescriptorReaded(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state: Int) {}
    fun onGattChanged(bluetoothGatt: BluetoothGatt?) {}
}