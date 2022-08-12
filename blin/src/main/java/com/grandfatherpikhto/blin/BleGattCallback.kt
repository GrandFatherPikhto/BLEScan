package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import com.grandfatherpikhto.blin.buffer.GattData
import com.grandfatherpikhto.blin.buffer.OutputBuffer
import com.grandfatherpikhto.blin.helper.hasFlag
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.nio.ByteBuffer

class BleGattCallback constructor(private val bleGattManager: BleGattManager,
                                  dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : BluetoothGattCallback() {

    private val tagLog = this.javaClass.simpleName
    private val scope = CoroutineScope(dispatcher)
    private val outputBuffer = OutputBuffer(this, dispatcher)

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        bleGattManager.onConnectionStateChange(gatt, status, newState)
        super.onConnectionStateChange(gatt, status, newState)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        gatt?.let { bluetoothGatt ->
            outputBuffer.bluetoothGatt = bluetoothGatt
        }
        bleGattManager.onGattDiscovered(gatt, status)
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        outputBuffer.onCharacteristicWrite(gatt, characteristic, status)
        bleGattManager.onCharacteristicWrite(gatt, characteristic, status)
        super.onCharacteristicWrite(gatt, characteristic, status)
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        outputBuffer.onDescriptorWrite(gatt, descriptor, status)
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
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorRead(gatt, descriptor, status)
        bleGattManager.onDescriptorRead(gatt, descriptor, status)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        characteristic?.let { char ->
            Log.d(tagLog, "onCharacteristicChanged(${char.uuid.toString().uppercase()})")
            gatt?.let { gt ->
                bleGattManager.onCharacteristicChanged(gt, char)
            }
        }
    }

    override fun onServiceChanged(gatt: BluetoothGatt) {
        super.onServiceChanged(gatt)
    }

    fun writeGattData(gattData: GattData) = outputBuffer.writeGattData(gattData)
}