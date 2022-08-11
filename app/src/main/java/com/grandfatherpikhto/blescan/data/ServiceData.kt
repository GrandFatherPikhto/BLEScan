package com.grandfatherpikhto.blescan.data

import android.bluetooth.BluetoothGattService

data class ServiceData (var bluetoothGattService: BluetoothGattService,
                        var opened: Boolean = false)