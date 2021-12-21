package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtInputQueue(private val btLeService: BtLeInterface) {
    companion object {
        const val TAG:String = "BtOutputQueue"
        const val BUFFER_SIZE = 0x100
    }

    private val buffer: MutableList<Triple<UUID, UUID?, ByteArray>> = mutableListOf()
    private var bluetoothGatt:BluetoothGatt? = null
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()
    private val btIoInterface: BtIoInterface by BtIoInterfaceLazy()
    private val bluetoothListener = object: BluetoothListener {
        override fun onGattChanged(bluetoothGatt: BluetoothGatt?) {
            super.onGattChanged(bluetoothGatt)
            this@BtInputQueue.bluetoothGatt = bluetoothGatt
            buffer.clear()
        }

        override fun onCharacteristicReaded(
            bluetoothGatt: BluetoothGatt?,
            bluetoothGattCharacteristic: BluetoothGattCharacteristic?,
            state: Int
        ) {
            super.onCharacteristicReaded(bluetoothGatt, bluetoothGattCharacteristic, state)
            Log.d(TAG, "onCharacteristicReaded: ${bluetoothGattCharacteristic?.uuid} state: $state")
            bluetoothGattCharacteristic?.let { characteristic ->
                characteristic.value?.let { value ->
                    if(state == BluetoothGatt.GATT_SUCCESS && value.isNotEmpty()) {
                        buffer.add(Triple(characteristic.uuid, null, value))
                        Log.d(TAG, "uuid: ${characteristic.uuid}, ${String(characteristic.value)}, buffer: ${buffer.size}")
                        btIoInterface.characteristicReaded(characteristic.uuid)
                    }
                }

            }
        }

        override fun onDescriptorReaded(
            bluetoothGatt: BluetoothGatt?,
            bluetoothGattDescriptor: BluetoothGattDescriptor?,
            state: Int
        ) {
            super.onDescriptorReaded(bluetoothGatt, bluetoothGattDescriptor, state)
            bluetoothGatt?.let { gatt ->
                bluetoothGattDescriptor?.let { descriptor ->
                    descriptor.characteristic?.let { characteristic ->
                        buffer.add(Triple(characteristic.uuid, descriptor.uuid, descriptor.value))
                    }
                }
            }
        }
    }

    init {
        Log.d(TAG, "Init")
        bluetoothInterface.addListener(bluetoothListener)
        btIoInterface.btInputQueue = this
    }

    /**
     *
     */
    private fun readValue(charUuid: UUID, descrUuid: UUID? = null, last:Boolean = false) : ByteArray {
        Log.d(TAG, "readValue buffer: ${buffer.size}")
        if(buffer.isEmpty()) return ByteArray(0)
        if(last) {
            buffer.last { data ->
                val res = data.first == charUuid && data.second == descrUuid
                buffer.removeAll { it.first == charUuid && it.second == descrUuid }
                if(res) {
                    return data.third
                }
                res
            }
        } else {
            buffer.first { data ->
                val res = data.first == charUuid && data.second == descrUuid
                if(res) {
                   buffer.remove(data)
                   return data.third
                }
                res
            }

        }
        return ByteArray(0)
    }

    fun readCharacteristic(charUuid: UUID, last:Boolean = false) = readValue(charUuid, null, last)
    fun readDescriptor(charUuid: UUID, descrUuid: UUID, last: Boolean = false) = readValue(charUuid, descrUuid, last)

    fun requestCharacteristic(charUuid: UUID): Boolean {
        bluetoothGatt?.let { gatt ->
            gatt.services.forEach { service ->
                service.characteristics.forEach { characteristic ->
                    if(characteristic.uuid == charUuid) {
                        return gatt.readCharacteristic(characteristic)
                    }
                }
            }
        }
        return false
    }

    fun destroy() {
        btIoInterface.btInputQueue = null
        bluetoothInterface.removeListener(bluetoothListener)
    }
}