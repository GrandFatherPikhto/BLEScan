package com.grandfatherpikhto.blin.data

import android.bluetooth.BluetoothDevice
import com.grandfatherpikhto.blin.orig.AbstractBleBondManager

data class BleBondState (val bluetoothDevice: BluetoothDevice, val state: AbstractBleBondManager.State) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleBondState

        if (bluetoothDevice.address != other.bluetoothDevice.address) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bluetoothDevice.hashCode()
        result = 31 * result + state.hashCode()
        return result
    }
}