package com.grandfatherpikhto.blescan.data

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.grandfatherpikhto.blin.buffer.GattData
import com.grandfatherpikhto.blin.data.BleGatt
import com.grandfatherpikhto.blin.helper.toHexString
import java.util.*

class BleItem ( val uuidService: UUID,
                val uuidCharacteristic: UUID? = null,
                val uuidDescriptor: UUID? = null,
                var value:ByteArray? = null,
                var opened: Boolean = false,
                var serviceType: Int = 0,
                var charProperties: Int = 0,
                var charWriteType: Int = 0,
                var descrPermission: Int = 0,
                var charNotify: Boolean = false,
) {
    enum class Type (val value: Int){
        Unknown(-1),
        Service(0x01),
        Characteristic(0x02),
        Descriptor(0x03);

        companion object {
            fun byValue(value: Int) : Type =
                values().first { it.value == value }
        }
    }

    constructor(bluetoothGattService: BluetoothGattService) :
            this(uuidService = bluetoothGattService.uuid,
                serviceType = bluetoothGattService.type)

    constructor(bluetoothGattCharacteristic: BluetoothGattCharacteristic,
                notify: Boolean = false) : this (
                    uuidService = bluetoothGattCharacteristic.service.uuid,
                    uuidCharacteristic = bluetoothGattCharacteristic.uuid,
                    value = bluetoothGattCharacteristic.value,
                    charProperties = bluetoothGattCharacteristic.properties,
                    charWriteType = bluetoothGattCharacteristic.writeType,
                    charNotify = notify)
    constructor(bluetoothGattDescriptor: BluetoothGattDescriptor) : this (
                uuidService = bluetoothGattDescriptor.characteristic.service.uuid,
                uuidCharacteristic = bluetoothGattDescriptor.characteristic.uuid,
                uuidDescriptor = bluetoothGattDescriptor.uuid,
                value = bluetoothGattDescriptor.value,
                descrPermission = bluetoothGattDescriptor.permissions)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleItem

        if (uuidService != other.uuidService) return false
        if (uuidCharacteristic != other.uuidCharacteristic) return false
        if (uuidDescriptor != other.uuidDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuidService.hashCode()
        result = 31 * result + (uuidCharacteristic?.hashCode() ?: 0)
        result = 31 * result + (uuidDescriptor?.hashCode() ?: 0)
        return result
    }

    val type: Type
        get() =
            if (uuidCharacteristic != null && uuidDescriptor != null) Type.Descriptor
            else if (uuidCharacteristic != null && uuidDescriptor == null) Type.Characteristic
            else Type.Service

    val bluetoothGattService:BluetoothGattService?
        get() =
            if (type == Type.Service) {
                BluetoothGattService(uuidCharacteristic, serviceType)
            } else null

    val bluetoothGattCharacteristic:BluetoothGattCharacteristic?
        get() =
            if (type == Type.Characteristic) {
                val char = BluetoothGattCharacteristic(uuidCharacteristic, charProperties, 0)
                char.value = value
                char
            } else null

    val bluetoothGattDescriptor: BluetoothGattDescriptor?
        get() = if (type == Type.Descriptor) {
            BluetoothGattDescriptor(uuidDescriptor, descrPermission)
        } else null

    val gattData: GattData
        get() =
            GattData(value = value,
            uuidService = uuidService,
            uuidCharacteristic = uuidCharacteristic,
            uuidDescriptor = uuidDescriptor)

    fun inverseOpened() {
        opened = !opened
    }

    override fun toString(): String
        = "BleItem(type: $type, service: $uuidService, characteristic: $uuidCharacteristic" +
            ", descriptor: $uuidDescriptor, value: ${value?.toHexString()})"
}