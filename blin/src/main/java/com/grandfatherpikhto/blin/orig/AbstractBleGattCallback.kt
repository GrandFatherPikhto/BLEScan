package com.grandfatherpikhto.blin.orig

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.grandfatherpikhto.blin.data.BleGattItem
import com.grandfatherpikhto.blin.buffer.QueueBuffer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class AbstractBleGattCallback constructor(private val bleGattManager: AbstractBleGattManager,
                                                   dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : BluetoothGattCallback() {

    private val tagLog = this.javaClass.simpleName
    private val scope = CoroutineScope(dispatcher)
    private val queueBuffer = QueueBuffer(dispatcher)

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        bleGattManager.onConnectionStateChange(gatt, status, newState)
        super.onConnectionStateChange(gatt, status, newState)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        gatt?.let { bluetoothGatt ->
            queueBuffer.bluetoothGatt = bluetoothGatt
        }
        bleGattManager.onGattDiscovered(gatt, status)
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        queueBuffer.onCharacteristicWrite(gatt, characteristic, status)
        bleGattManager.onCharacteristicWrite(gatt, characteristic, status)
        super.onCharacteristicWrite(gatt, characteristic, status)
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        queueBuffer.onDescriptorWrite(gatt, descriptor, status)
        bleGattManager.onDescriptorWrite(gatt, descriptor, status)
        super.onDescriptorWrite(gatt, descriptor, status)
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        bleGattManager.onCharacteristicRead(gatt, characteristic, status)
        queueBuffer.onCharacteristicRead(gatt, characteristic, status)
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorRead(gatt, descriptor, status)
        bleGattManager.onDescriptorRead(gatt, descriptor, status)
        queueBuffer.onDescriptorRead(gatt, descriptor, status)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        characteristic?.let { char ->
            gatt?.let { gt ->
                bleGattManager.onCharacteristicChanged(gt, char)
            }
        }
    }

    override fun onServiceChanged(gatt: BluetoothGatt) {
        super.onServiceChanged(gatt)
    }

    fun writeGattData(bleGattData: BleGattItem) = queueBuffer.addGattData(bleGattData)
}