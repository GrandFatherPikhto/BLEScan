package com.grandfatherpikhto.blescan.service

import android.bluetooth.*
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

/**
 * Обратные вызовы работы с GATT
 */
@DelicateCoroutinesApi
@InternalCoroutinesApi
class BtGattCallback  : BluetoothGattCallback() {
    companion object {
        const val TAG: String = "BtGattCallback"
        const val MAX_TRY_CONNECT = 6
    }
    
    interface GattCallback {
        fun onChangeState(state:BtLeConnector.State) {}
        fun onGattDiscovered(bluetoothGatt: BluetoothGatt) {}
        fun onCharacteristicWrited(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?, state: Int) {}
        fun onCharacteristicReaded(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?, state: Int) {}
        fun onCharacteristicChanged(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?) {}
        fun onDescriptorWrited(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state: Int) {}
        fun onDescriptorReaded(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state: Int) {}
        fun onServiceChanged(bluetoothGatt: BluetoothGatt?) {}
    }

    private var gattCallbacks:MutableList<GattCallback> = mutableListOf()

    private var state:BtLeConnector.State = BtLeConnector.State.Unknown

    private var bluetoothGatt:BluetoothGatt? = null

    private var tryConnectCounter = 0

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

        super.onConnectionStateChange(gatt, status, newState)
        when (newState) {
            BluetoothProfile.STATE_DISCONNECTED -> {
                changeState(BtLeConnector.State.Disconnected)
            }
            BluetoothProfile.STATE_CONNECTING -> {
                gattCallbacks.forEach { callback ->
                    callback.onChangeState(BtLeConnector.State.Connecting)
                }
            }
            BluetoothProfile.STATE_CONNECTED -> {
                changeState(BtLeConnector.State.Connected)
                bluetoothGatt = gatt
                if(gatt!!.discoverServices()) {
                    changeState(BtLeConnector.State.Discovering)
                } else {
                    changeState(BtLeConnector.State.Error)
                }
                tryConnectCounter = 0
            }
            BluetoothProfile.STATE_DISCONNECTING -> {
                gattCallbacks.forEach { callback ->
                    callback.onChangeState(BtLeConnector.State.Disconnecting)
                }
            }
            else -> {
            }
        }
        if(status == 6 || status == 133) {
            Log.d(TAG, "onConnectionStateChange $status $newState запустить рескан")
            if (tryConnectCounter >= MAX_TRY_CONNECT - 1) {
                tryConnectCounter = 0
                gattCallbacks.forEach { callback ->
                    callback.onChangeState(BtLeConnector.State.FatalError)
                }
            } else {
                gattCallbacks.forEach { callback ->
                    callback.onChangeState(BtLeConnector.State.Error)
                }
                tryConnectCounter++
            }
        }
    }

    override fun onServicesDiscovered(btgatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(btgatt, status)
        state = BtLeConnector.State.Discovered
        if(status == BluetoothGatt.GATT_SUCCESS) {
            if(btgatt != null) {
                gattCallbacks.forEach { callback ->
                    callback.onChangeState(state)
                    callback.onGattDiscovered(btgatt)
                }
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        state = BtLeConnector.State.CharWrited
        gattCallbacks.forEach { callback ->
            callback.onChangeState(state)
            callback.onCharacteristicWrited(gatt, characteristic, status)
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        state = BtLeConnector.State.CharReaded
        gattCallbacks.forEach { callback ->
            callback.onChangeState(state)
            callback.onCharacteristicReaded(gatt, characteristic, status)
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        state = BtLeConnector.State.CharWrited
        gattCallbacks.forEach { callback ->
            callback.onChangeState(state)
            callback.onCharacteristicChanged(gatt, characteristic)
        }
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorRead(gatt, descriptor, status)
        state = BtLeConnector.State.DescrReaded
        gattCallbacks.forEach { callback ->
            callback.onChangeState(state)
            callback.onDescriptorReaded(gatt, descriptor, status)
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)
        state = BtLeConnector.State.DescrWrited
        gattCallbacks.forEach { callback ->
            callback.onChangeState(state)
            callback.onDescriptorWrited(gatt, descriptor, status)
        }
    }

    override fun onServiceChanged(gatt: BluetoothGatt) {
        super.onServiceChanged(gatt)
        state = BtLeConnector.State.ServiceChanged
        gattCallbacks.forEach { callback ->
            callback.onChangeState(state)
            callback.onServiceChanged(gatt)
        }
    }

    fun addEventListener(callback: GattCallback) {
        gattCallbacks.add(callback)
    }

    private fun changeState(value: BtLeConnector.State) {
        state = value
        gattCallbacks.forEach { callback ->
            callback.onChangeState(value)
        }
    }
}