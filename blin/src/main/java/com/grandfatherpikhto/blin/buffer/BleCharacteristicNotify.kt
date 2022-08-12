package com.grandfatherpikhto.blin.buffer

import android.bluetooth.BluetoothGattCharacteristic

data class BleCharacteristicNotify(val bluetoothGattCharacteristic: BluetoothGattCharacteristic, val notify: Boolean)
