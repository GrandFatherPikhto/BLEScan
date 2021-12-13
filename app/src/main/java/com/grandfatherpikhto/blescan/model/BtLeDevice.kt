package com.grandfatherpikhto.blescan.model

import android.bluetooth.BluetoothDevice

data class BtLeDevice(
    val address: String = "00:00:00:00:00:00",
    val name: String? = "Unknown Device",
    val bondState: Int = BluetoothDevice.BOND_NONE)

/**
 * Сопроводительный конвертер для преобразования экземпляра BluetoothDevice
 * в упрощённую "выжимку", достаточную для работы с устройством
 */
fun BluetoothDevice.toBtLeDevice(defaultAddress: String = "00:00:00:00:00:00", defaultName:String = "Unknown Device"): BtLeDevice {
    return BtLeDevice(this.address ?: defaultAddress, this.name ?: defaultName, this.bondState)
}
