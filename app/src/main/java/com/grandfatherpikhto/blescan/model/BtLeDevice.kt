package com.grandfatherpikhto.blescan.model

import android.bluetooth.BluetoothDevice

data class BtLeDevice(val address: String, val name: String?, val bondState: Int)
