package com.grandfatherpikhto.blescan.service

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

@DelicateCoroutinesApi
@InternalCoroutinesApi
class BtCharIO (private val btLeService: BtLeService) {
    private val bluetoothInterface:BluetoothInterface by BluetoothInterfaceLazy()

    private var bluetoothGatt:BluetoothGatt? = null

    private val characteristics: MutableList<BluetoothGattCharacteristic> = mutableListOf()
    private val descriptors: MutableList<BluetoothGattDescriptor> = mutableListOf()

    enum class WriteState(val value:Int) {
        Unknown(0x00),
        Writing(0x01),
        Writed(0x02)
    }

    enum class ReadState(val value:Int) {
        Unknown(0x00),
        Reading(0x01),
        Readed(0x02)
    }

    private var writeState: WriteState by Delegates.observable(WriteState.Unknown) { _, _, state ->
        if(state == WriteState.Writed) {
            writeNextChar()
            writeNextDescr()
        }
    }

    private var readState:ReadState by Delegates.observable(ReadState.Unknown) { _, _, state ->
        if(state == ReadState.Reading) {
            readNextChar()
            readNextDescr()
        }
    }

    data class CharQueue(val characteristic: BluetoothGattCharacteristic, val value: ByteArray)
    data class DescrQueue(val descriptor: BluetoothGattDescriptor, val value: ByteArray)

    private val outputChars: MutableList<Pair<BluetoothGattCharacteristic, ByteArray>>  = mutableListOf()
    private val outputDescr: MutableList<Pair<BluetoothGattDescriptor, ByteArray>> = mutableListOf()
    private val inputChars: MutableList<Pair<BluetoothGattCharacteristic, ByteArray>> = mutableListOf()
    private val inputDescr: MutableList<Pair<BluetoothGattDescriptor, ByteArray>> = mutableListOf()

    private val bluetoothListener = object: BluetoothListener {
        override fun onGattChanged(bluetoothGatt: BluetoothGatt?) {
            super.onGattChanged(bluetoothGatt)
            this@BtCharIO.bluetoothGatt = bluetoothGatt
            characteristics.clear()
            descriptors.clear()
            bluetoothGatt?.services?.forEach { service ->
                service?.characteristics?.forEach { characteristic ->
                    characteristics.add(characteristic)
                    characteristic?.descriptors?.forEach { descriptor ->
                        descriptors.add(descriptor)
                    }
                }
            }
        }

        override fun onCharacteristicReaded(
            bluetoothGatt: BluetoothGatt?,
            bluetoothGattCharacteristic: BluetoothGattCharacteristic?,
            state: Int
        ) {
            super.onCharacteristicReaded(bluetoothGatt, bluetoothGattCharacteristic, state)
            readState = ReadState.Reading
        }

        override fun onDescriptorReaded(
            bluetoothGatt: BluetoothGatt?,
            bluetoothGattDescriptor: BluetoothGattDescriptor?,
            state: Int
        ) {
            super.onDescriptorReaded(bluetoothGatt, bluetoothGattDescriptor, state)
            readState = ReadState.Reading
        }

        override fun onCharacteristicWrited(
            bluetoothGatt: BluetoothGatt?,
            bluetoothGattCharacteristic: BluetoothGattCharacteristic?,
            state: Int
        ) {
            super.onCharacteristicWrited(bluetoothGatt, bluetoothGattCharacteristic, state)
            writeState = WriteState.Writed
        }

        override fun onDescriptorWrited(
            bluetoothGatt: BluetoothGatt?,
            bluetoothGattDescriptor: BluetoothGattDescriptor?,
            state: Int
        ) {
            super.onDescriptorWrited(bluetoothGatt, bluetoothGattDescriptor, state)
            writeState = WriteState.Writed
        }
    }

    private fun MutableList<BluetoothGattCharacteristic>.getCharacteristic(uuid: String): BluetoothGattCharacteristic? {
        val searchUuid = UUID.fromString(uuid)
        return this.find { characteristic ->
            characteristic.uuid == searchUuid
        }
    }

    private fun MutableList<BluetoothGattDescriptor>.getDescriptor(uuid: String): BluetoothGattDescriptor? {
        val searchUuid = UUID.fromString(uuid)
        return this.find { descriptor ->
            descriptor.uuid == searchUuid
        }
    }

    /**
     *
     */
    fun writeCharacteristic(uuid: String, value: ByteArray) {
        characteristics.getCharacteristic(uuid)?.let { characteristic ->
            outputChars.add(Pair<BluetoothGattCharacteristic, ByteArray>(characteristic, value))
            writeNextChar()
        }
    }

    /**
     *
     */
    fun writeDescriptor(uuid: String, value: ByteArray) {
        descriptors.getDescriptor(uuid)?.let { descriptor ->
            outputDescr.add(Pair<BluetoothGattDescriptor, ByteArray>(descriptor, value))
            writeNextDescr()
        }
    }

    /**
     *
     */
    private fun writeNextChar() {
        if(writeState != WriteState.Writing && outputChars.size > 0) {
            outputChars.first { data ->
                data.first.value = data.second
                bluetoothGatt?.let { gatt ->
                    if (gatt.writeCharacteristic(data.first)) {
                        outputChars.removeFirst()
                        writeState = WriteState.Writing
                    }
                }
                true
            }
        }
    }

    /**
     *
     */
    private fun writeNextDescr() {
        if(writeState != WriteState.Writing && outputDescr.size > 0) {
            outputDescr.first { data ->
                data.first.value = data.second
                bluetoothGatt?.let { gatt ->
                    if (gatt.writeDescriptor(data.first)) {
                        outputDescr.removeFirst()
                        writeState = WriteState.Writing
                    }
                }
                true
            }
        }
    }

    /**
     *
     */
    private fun readNextChar() {

    }

    /**
     *
     */
    private fun readNextDescr() {

    }
    /**
     *
     */

    init {
        bluetoothInterface.addListener(bluetoothListener)
    }

    /**
     * Перед уничтожением объекта, удаляем листенер из списка прослушивания
     */
    fun destroy() {
        bluetoothInterface.removeListener(bluetoothListener)
    }
}

