package com.grandfatherpikhto.blin.buffer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.grandfatherpikhto.blin.BleGattCallback
import com.grandfatherpikhto.blin.data.GattData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlin.properties.Delegates

class OutputBuffer (private val bleGattCallback: BleGattCallback,
                    dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val tagLog = this.javaClass.simpleName
    private val scope = CoroutineScope(dispatcher)
    private val buffer = MutableListQueue<GattData>()
    private val bufferMutex = Mutex(locked = false)

    var bluetoothGatt:BluetoothGatt? by Delegates.observable(null) { _, _, newValue ->
        newValue?.let { _ ->
            buffer.peek()?.let { nextGattData ->
                writeNextGattData(nextGattData)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeNextCharacteristic(gattData: GattData) : Boolean {
        bluetoothGatt?.let { gatt ->
            if (gattData.uuidDescriptor == null) {
                gatt.getService(gattData.uuidService)?.let { service ->
                    service.getCharacteristic(gattData.uuidCharacteristic)?.let { characteristic ->
                        characteristic.value = gattData.value
                        return gatt.writeCharacteristic(characteristic)
                    }
                }
            }
        }

        return false
    }

    @SuppressLint("MissingPermission")
    private fun writeNextDescriptor(gattData: GattData) : Boolean {
        bluetoothGatt?.let { gatt ->
            gatt.getService(gattData.uuidService)?.let { service ->
                service.getCharacteristic(gattData.uuidCharacteristic)?.let { characteristic ->
                    characteristic.getDescriptor(gattData.uuidDescriptor)?.let { descriptor ->
                        descriptor.value = gattData.value
                        return gatt.writeDescriptor(descriptor)
                    }
                }
            }
        }

        return false
    }

    private fun writeNextGattData(gattData: GattData) : Boolean {
        return if (gattData.uuidDescriptor == null) {
            writeNextCharacteristic(gattData)
        } else {
            writeNextDescriptor(gattData)
        }
    }

    private fun dequeueAndWriteNextGattData(gattData: GattData) {
        if (buffer.peek() == gattData) {
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
                dequeueAndWriteNextGattData(GattData(characteristic))
            }
        } else {
            if (gatt != null && characteristic != null) {
                writeNextGattData(GattData(characteristic))
            }
        }
    }

    fun onDescriptorWrite (gatt: BluetoothGatt?,
                           descriptor: BluetoothGattDescriptor?,
                           status: Int?) {

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (gatt != null && descriptor != null) {
                dequeueAndWriteNextGattData(GattData(descriptor))
            }
        } else {
            if (gatt != null && descriptor != null) {
                writeNextGattData(GattData(descriptor))
            }
        }
    }

    fun writeGattData(gattData: GattData) {
        buffer.enqueue(gattData)
        if (buffer.count == 1) {
            writeNextGattData(gattData)
        }
    }
}