package com.grandfatherpikhto.blin.data

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.*

data class BleGattItem (val uuidService: UUID,
                        val uuidCharacteristic: UUID? = null,
                        val uuidDescriptor: UUID? = null,
                        val value:ByteArray? = null,
                        val type: Type = Type.Write
) {
    enum class Type (val value: Int) {
        Write(0x01),
        Read(0x02),
    }

    constructor(bluetoothGattService: BluetoothGattService, type: Type) : this(
                uuidService = bluetoothGattService.uuid,
                type = type
            )

    constructor(bluetoothGattCharacteristic: BluetoothGattCharacteristic, type: Type) :
            this(uuidService = bluetoothGattCharacteristic.service.uuid,
                uuidCharacteristic = bluetoothGattCharacteristic.uuid,
                value = bluetoothGattCharacteristic.value,
                type = type
            )

    constructor(bluetoothGattDescriptor: BluetoothGattDescriptor, type: Type) :
            this(uuidService = bluetoothGattDescriptor.characteristic.service.uuid,
                uuidCharacteristic = bluetoothGattDescriptor.characteristic.uuid,
                uuidDescriptor = bluetoothGattDescriptor.uuid,
                value = bluetoothGattDescriptor.value,
                type = type
            )

    fun getService(bluetoothGatt: BluetoothGatt) : BluetoothGattService? =
        bluetoothGatt.getService(uuidService)

    fun getCharacteristic(bluetoothGatt: BluetoothGatt) : BluetoothGattCharacteristic? =
        if (uuidCharacteristic == null) {
            null
        } else {
            bluetoothGatt.getService(uuidService)
                ?.getCharacteristic(uuidService)
        }

    fun getDescriptor(bluetoothGatt: BluetoothGatt) : BluetoothGattDescriptor? =
        if (uuidCharacteristic == null && uuidDescriptor == null) {
            null
        } else {
            bluetoothGatt.getService(uuidService)?.let { service ->
                service.getCharacteristic(uuidCharacteristic)
                    ?.getDescriptor(uuidDescriptor)
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleGattItem

        if (type != other.type) return false
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
}