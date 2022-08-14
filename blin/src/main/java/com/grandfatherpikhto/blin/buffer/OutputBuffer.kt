package com.grandfatherpikhto.blin.buffer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.grandfatherpikhto.blin.BleGattCallback
import com.grandfatherpikhto.blin.data.BleGattItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlin.properties.Delegates

class OutputBuffer (private val bleGattCallback: BleGattCallback,
                    dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val tagLog = this.javaClass.simpleName
    private val scope = CoroutineScope(dispatcher)
    private val buffer = MutableListQueue<BleGattItem>()
    private val bufferMutex = Mutex(locked = false)

    var bluetoothGatt:BluetoothGatt? by Delegates.observable(null) { _, _, newValue ->
        newValue?.let { _ ->
            buffer.peek()?.let { nextGattData ->
                writeNextGattData(nextGattData)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeNextCharacteristic(bleGattItem: BleGattItem) : Boolean {
        bluetoothGatt?.let { gatt ->
            bleGattItem.getCharacteristic(gatt)?.let { characteristic ->
                characteristic.value = bleGattItem.value
                return gatt.writeCharacteristic(characteristic)
            }
        }

        return false
    }

    @SuppressLint("MissingPermission")
    private fun writeNextDescriptor(bleGattItem: BleGattItem) : Boolean {
        bluetoothGatt?.let { gatt ->
            bleGattItem.getDescriptor(gatt)?.let { descriptor ->
                descriptor.value = bleGattItem.value
                return gatt.writeDescriptor(descriptor)
            }
        }

        return false
    }

    private fun writeNextGattData(bleGattData: BleGattItem) : Boolean {
        return if (bleGattData.uuidDescriptor == null) {
            writeNextCharacteristic(bleGattData)
        } else {
            writeNextDescriptor(bleGattData)
        }
    }

    private fun dequeueAndWriteNextGattData(bleGattData: BleGattItem) {
        if (buffer.peek() == bleGattData) {
            buffer.dequeue()
            buffer.peek()?.let { nextGattData ->
                writeNextGattData(nextGattData)
            }
        }
    }

    fun onCharacteristicWrite (gatt: BluetoothGatt?,
                               characteristic: BluetoothGattCharacteristic?,
                               status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (gatt != null && characteristic != null) {
                dequeueAndWriteNextGattData(BleGattItem(characteristic))
            }
        } else {
            if (gatt != null && characteristic != null) {
                writeNextGattData(BleGattItem(characteristic))
            }
        }
    }

    fun onDescriptorWrite (gatt: BluetoothGatt?,
                           descriptor: BluetoothGattDescriptor?,
                           status: Int?) {

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (gatt != null && descriptor != null) {
                dequeueAndWriteNextGattData(BleGattItem(descriptor))
            }
        } else {
            if (gatt != null && descriptor != null) {
                writeNextGattData(BleGattItem(descriptor))
            }
        }
    }

    fun writeGattData(bleGattItem: BleGattItem) {
        buffer.enqueue(bleGattItem)
        if (buffer.count == 1) {
            writeNextGattData(bleGattItem)
        }
    }
}