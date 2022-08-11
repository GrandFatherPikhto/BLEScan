package com.grandfatherpikhto.blescan.data

import android.bluetooth.BluetoothGattCharacteristic
import com.grandfatherpikhto.blescan.ui.adapters.RvBleDeviceAdapter

data class CharacteristicData (var bluetoothGattCharacteristic: BluetoothGattCharacteristic,
                               var format: RvBleDeviceAdapter.Format = RvBleDeviceAdapter.Format.Bytes,
                               var visible: Boolean = false)