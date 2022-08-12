package com.grandfatherpikhto.blescan.data

import android.bluetooth.BluetoothGattCharacteristic
import com.grandfatherpikhto.blin.buffer.BleCharacteristicNotify

data class CharacteristicData (var bluetoothGattCharacteristic: BluetoothGattCharacteristic,
                               var notify: Boolean = false,
                               var visible: Boolean = false)