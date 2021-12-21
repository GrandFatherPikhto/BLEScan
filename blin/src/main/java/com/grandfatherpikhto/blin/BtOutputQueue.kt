package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*
import kotlin.properties.Delegates

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtOutputQueue(private val btLeInterface: BtLeInterface) {
    companion object {
        const val TAG:String = "BtInputQueue"
        const val BUFFER_SIZE = 0x100
    }

    private var writing by Delegates.observable(false) { _, _, newValue ->
        if(!newValue) {
            writeNext()
        }
    }

    private val buffer: MutableList<Triple<UUID, UUID?, ByteArray>> = mutableListOf()

    private var bluetoothGatt:BluetoothGatt? = null

    private val bluetoothInterface by BluetoothInterfaceLazy()
    private val btIoInterface: BtIoInterface by BtIoInterfaceLazy()

    private val bluetoothListener = object: BluetoothListener {
        override fun onGattChanged(bluetoothGatt: BluetoothGatt?) {
            super.onGattChanged(bluetoothGatt)
            this@BtOutputQueue.bluetoothGatt = bluetoothGatt
            buffer.clear()
        }

        override fun onCharacteristicWrite (
            bluetoothGatt: BluetoothGatt?,
            bluetoothGattCharacteristic: BluetoothGattCharacteristic?,
            state: Int
        ) {
            super.onCharacteristicWrite(bluetoothGatt, bluetoothGattCharacteristic, state)
            if(state == BluetoothGatt.GATT_SUCCESS) {
                bluetoothGattCharacteristic?.let { characteristic ->
                    buffer.remove(Triple(characteristic.uuid, null, characteristic.value))
                }
            }
            writing = false
        }

        override fun onDescriptorWrite (
            bluetoothGatt: BluetoothGatt?,
            bluetoothGattDescriptor: BluetoothGattDescriptor?,
            state: Int
        ) {
            super.onDescriptorWrite(bluetoothGatt, bluetoothGattDescriptor, state)
            writing = false
        }
    }

    init {
        bluetoothInterface.addListener(bluetoothListener)
        btIoInterface.btOutputQueue = this
    }

    /**
     * Опрашивает буффер и если есть значения, записывает их в характеристику или дескриптор
     * Удаление из буффера произойдут только после того, как будет подтверждено, что
     * характеристика или дескриптор записаны, т.е., по событиям
     * onCharacteristicWrited или onDescriptorWrited
     */
    private fun writeNext(): Boolean {
        bluetoothGatt?.let { gatt ->
            if(!(this.writing || buffer.size <= 0)) {
                buffer.first { data ->
                    gatt.services.forEach { service ->
                        service.characteristics.find { characteristic ->
                            val res = characteristic.uuid == data.first
                            if(data.second == null) {
                                characteristic.value = data.third
                                val res = gatt.writeCharacteristic(characteristic)
                                // val out = characteristic.value.map { String.format("%02X", it).toString() }
                                // Log.d(TAG, "Write[$res]: ${characteristic.uuid}, $out")
                            } else {
                                characteristic.descriptors.find { descriptor ->
                                    val descrRes = descriptor.uuid == data.second
                                    descriptor.value = data.third
                                    return gatt.writeDescriptor(descriptor)
                                    descrRes
                                }
                            }
                            res
                        }
                    }
                    true
                }
            }
        }

        return false
    }

    /**
     *
     */
    private fun prepareBuffer(
        charUuid: UUID,
        descrUuid: UUID? = null,
        last: Boolean = false) {
        if(last) {
            buffer.removeAll { it.first == charUuid && it.second == descrUuid }
        } else {
            if(buffer.filter { it.first == charUuid && it.second == descrUuid }.size >= BUFFER_SIZE - 1) {
                buffer.last { it.first == charUuid && it.second == descrUuid }
                    .let {
                        buffer.remove(it)
                    }
            }
        }
    }

    /**
     *
     */
    private fun addBuffer(
        charUuid: UUID,
        descrUuid: UUID? = null,
        value: ByteArray ,last: Boolean = false) : Boolean {
        prepareBuffer(charUuid, descrUuid, last)
        bluetoothGatt?.let { gatt ->
            gatt.services.forEach { service ->
                service.characteristics.find { characteristic ->
                    val charRes = characteristic.uuid == charUuid
                    if(descrUuid == null && charRes) {
                        buffer.add(Triple(charUuid, null, value))
                        writeNext()
                        return true
                    } else if (charRes) {
                        characteristic.descriptors.find { descriptor ->
                            val descrRes = descriptor.uuid == descrUuid
                            if(descrRes) {
                                buffer.add(Triple(charUuid, descrUuid, value))
                                writeNext()
                                return true
                            }
                            descrRes
                        }
                    }
                    charRes
                }
            }
        }

        return false
    }

    fun writeCharacteristic(uuid: UUID, value: ByteArray, last: Boolean = false) = addBuffer(uuid, null, value, last)

    fun writeDescriptor(charUuid: UUID, descrUuid: UUID, value: ByteArray, last: Boolean = false) = addBuffer(charUuid, descrUuid, value, last)

    fun destroy() {
        bluetoothInterface.removeListener(bluetoothListener)
        btIoInterface.btOutputQueue = null
    }
}