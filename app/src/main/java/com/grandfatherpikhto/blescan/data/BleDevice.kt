package com.grandfatherpikhto.blescan.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid

data class BleDevice(val address: String,
                     val name: String? = null,
                     val bondState: Int = BluetoothDevice.BOND_NONE,
                     val uuids: List<ParcelUuid> = listOf()) {
    @SuppressLint("MissingPermission")
    constructor(bluetoothDevice: BluetoothDevice)
            : this( bluetoothDevice.address,
        bluetoothDevice.name,
        bluetoothDevice.bondState,
        bluetoothDevice.uuids?.toList() ?: listOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleDevice

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

}