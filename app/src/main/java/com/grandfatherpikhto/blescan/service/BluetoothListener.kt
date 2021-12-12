package com.grandfatherpikhto.blescan.service

import android.bluetooth.*
import com.grandfatherpikhto.blescan.model.BtLeDevice
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.properties.Delegates

@InternalCoroutinesApi
@DelicateCoroutinesApi
interface BluetoothListener {
    fun onSetBluetoothAdapter(bluetoothAdapter: BluetoothAdapter?) {}
    fun onBluetoothPairingRequest(oldDevice: BluetoothDevice?, newDevice: BluetoothDevice?) {}
    fun onChangeBluetoothState(bluetoothDevice: BluetoothDevice?, oldState: Int, newState: Int) {}
    fun onChangeBluetoothBondState(bluetoothDevice: BluetoothDevice?, oldState: Int, newState: Int) {}
    fun onSetBluetoothDevice(oldValue: BluetoothDevice?, newValue: BluetoothDevice?) {}
    fun onSetCurrentDevice(oldValue: BtLeDevice?, newValue: BtLeDevice?) {}
    fun onServiceBound(oldValue: BtLeService?, newValue: BtLeService?) {}
    fun onBluetoothEnabled(enabled:Boolean) {}
    fun onBluetoothPaired(btLeDevice: BtLeDevice?) {}
    fun onChangeScannerState(oldState: BtLeScanner.State, newState: BtLeScanner.State) {}
    fun onFindDevice(btLeDevice: BtLeDevice?) {}
    fun onScanError(oldError: Int, newError:Int) {}
    fun onChangeConnectorState(oldState: BtLeConnector.State, newState: BtLeConnector.State) {}
    fun onGattError(oldError:Int, newError:Int) {}
    fun onCharacteristicWrited(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?, state: Int) {}
    fun onCharacteristicReaded(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?, state: Int) {}
    fun onCharacteristicChanged(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?) {}
    fun onDescriptorWrited(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state: Int) {}
    fun onDescriptorReaded(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state: Int) {}
    fun onGattChanged(bluetoothGatt: BluetoothGatt?) {}
}