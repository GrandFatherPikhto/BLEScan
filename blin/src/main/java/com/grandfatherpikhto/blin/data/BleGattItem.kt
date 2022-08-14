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
) {
    constructor(bluetoothGattService: BluetoothGattService) :
            this(uuidService = bluetoothGattService.uuid,
            )

    constructor(bluetoothGattCharacteristic: BluetoothGattCharacteristic) :
            this(uuidService = bluetoothGattCharacteristic.service.uuid,
                uuidCharacteristic = bluetoothGattCharacteristic.uuid,
                value = bluetoothGattCharacteristic.value
            )

    constructor(bluetoothGattDescriptor: BluetoothGattDescriptor) :
            this(uuidService = bluetoothGattDescriptor.characteristic.service.uuid,
                uuidCharacteristic = bluetoothGattDescriptor.characteristic.uuid,
                uuidDescriptor = bluetoothGattDescriptor.uuid,
                value = bluetoothGattDescriptor.value,
            )

    fun getService(bluetoothGatt: BluetoothGatt) : BluetoothGattService? =
        bluetoothGatt.getService(uuidService)

    fun getCharacteristic(bluetoothGatt: BluetoothGatt) : BluetoothGattCharacteristic? =
        if (uuidCharacteristic == null) {
            null
        } else {
            bluetoothGatt.getService(uuidService)?.let { service ->
                service.getCharacteristic(uuidCharacteristic)
            }
        }

    fun getDescriptor(bluetoothGatt: BluetoothGatt) : BluetoothGattDescriptor? =
        if (uuidCharacteristic == null && uuidDescriptor == null) {
            null
        } else {
            bluetoothGatt.getService(uuidService)?.let { service ->
                service.getCharacteristic(uuidCharacteristic)?.let { characteristic ->
                    characteristic.getDescriptor(uuidDescriptor)
                }
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleGattItem

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