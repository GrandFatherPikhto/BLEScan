package com.grandfatherpikhto.blin

import android.bluetooth.*
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * Обратные вызовы работы с GATT
 */
@DelicateCoroutinesApi
@InternalCoroutinesApi
class LeGattCallback  : BluetoothGattCallback() {
    companion object {
        const val TAG: String = "BtGattCallback"
        const val MAX_TRY_CONNECT = 6
    }

    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()

    /** */
    private var tryConnectCounter = 0

    /**
     *
     */
    override fun onConnectionStateChange(btgatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(btgatt, status, newState)
        when (newState) {
            BluetoothProfile.STATE_DISCONNECTED -> {
                bluetoothInterface.connectorState = BtLeConnector.State.Disconnected
            }
            BluetoothProfile.STATE_CONNECTING -> {
                bluetoothInterface.connectorState = BtLeConnector.State.Connecting
            }
            BluetoothProfile.STATE_CONNECTED -> {
                bluetoothInterface.connectorState = BtLeConnector.State.Connected
                bluetoothInterface.bluetoothGatt = btgatt
                if(btgatt!!.discoverServices()) {
                    bluetoothInterface.connectorState = BtLeConnector.State.Discovering
                } else {
                    bluetoothInterface.connectorState = BtLeConnector.State.Error
                }
                tryConnectCounter = 0
            }
            BluetoothProfile.STATE_DISCONNECTING -> {
                bluetoothInterface.connectorState = BtLeConnector.State.Disconnecting
            }
            else -> {
            }
        }
        if(status == 6 || status == 133) {
            Log.d(TAG, "onConnectionStateChange $status $newState запустить рескан")
            if (tryConnectCounter >= MAX_TRY_CONNECT - 1) {
                tryConnectCounter = 0
                bluetoothInterface.connectorState = BtLeConnector.State.FatalError
            } else {
                bluetoothInterface.connectorState = BtLeConnector.State.Error
                tryConnectCounter++
            }
        }
    }

    override fun onServicesDiscovered(btgatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(btgatt, status)
        bluetoothInterface.connectorState = BtLeConnector.State.Discovered
        if(status == BluetoothGatt.GATT_SUCCESS) {
            if(btgatt != null) {
                bluetoothInterface.bluetoothGatt = btgatt
            }
        }
    }

    override fun onCharacteristicWrite(
        btgatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(btgatt, characteristic, status)
        bluetoothInterface.connectorState = BtLeConnector.State.CharWrited
        bluetoothInterface.characteristicWrite(btgatt, characteristic, status)
    }

    override fun onCharacteristicRead(
        btgatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(btgatt, characteristic, status)
        bluetoothInterface.connectorState = BtLeConnector.State.CharReaded
        bluetoothInterface.characteristicRead(btgatt, characteristic, status)
    }

    override fun onCharacteristicChanged(
        btgatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(btgatt, characteristic)
        bluetoothInterface.connectorState = BtLeConnector.State.CharChanged
        bluetoothInterface.characteristicChange(btgatt, characteristic)
    }

    override fun onDescriptorRead(
        btgatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorRead(btgatt, descriptor, status)
        bluetoothInterface.connectorState = BtLeConnector.State.DescrReaded
        bluetoothInterface.descriptorRead(btgatt, descriptor, status)
    }

    override fun onDescriptorWrite(
        btgatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorWrite(btgatt, descriptor, status)
        bluetoothInterface.connectorState = BtLeConnector.State.DescrWrited
        bluetoothInterface.descriptorWrite(btgatt, descriptor, status)
    }

    override fun onServiceChanged(btgatt: BluetoothGatt) {
        super.onServiceChanged(btgatt)
        bluetoothInterface.connectorState = BtLeConnector.State.CharChanged
    }

    override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
        super.onReliableWriteCompleted(gatt, status)
    }
}