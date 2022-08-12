package com.grandfatherpikhto.blin.buffer

import android.bluetooth.BluetoothGattCharacteristic
import java.util.*

data class BleCharacteristicNotify(val uuid: UUID, val notify: Boolean)
