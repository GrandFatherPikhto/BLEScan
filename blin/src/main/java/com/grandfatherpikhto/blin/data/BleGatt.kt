package com.grandfatherpikhto.blin.data

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService

class BleGatt constructor(private val bleDevice: BleDevice, val services: List<BluetoothGattService>){
    constructor(bluetoothGatt: BluetoothGatt) :
            this ( BleDevice(bluetoothGatt.device), bluetoothGatt.services)
    val device get() = bleDevice
}