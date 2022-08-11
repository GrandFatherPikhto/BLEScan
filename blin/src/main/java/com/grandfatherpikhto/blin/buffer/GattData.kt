package com.grandfatherpikhto.blin.buffer

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import java.util.*

data class GattData (val value: ByteArray,
                     val uuidService: UUID,
                     val uuidCharacteristic: UUID,
                     val uuidDescriptor: UUID? = null) {

    constructor(bluetoothGattCharacteristic: BluetoothGattCharacteristic)
        : this(bluetoothGattCharacteristic.value,
            bluetoothGattCharacteristic.service.uuid,
            bluetoothGattCharacteristic.uuid)

    constructor(bluetoothGattDescriptor: BluetoothGattDescriptor)
            : this(bluetoothGattDescriptor.value,
        bluetoothGattDescriptor.characteristic.service.uuid,
        bluetoothGattDescriptor.characteristic.uuid, bluetoothGattDescriptor.uuid)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GattData

        if (!value.contentEquals(other.value)) return false
        if (uuidService != other.uuidService) return false
        if (uuidCharacteristic != other.uuidCharacteristic) return false
        if (uuidDescriptor != other.uuidDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.contentHashCode()
        result = 31 * result + uuidService.hashCode()
        result = 31 * result + uuidCharacteristic.hashCode()
        result = 31 * result + (uuidDescriptor?.hashCode() ?: 0)
        return result
    }
}