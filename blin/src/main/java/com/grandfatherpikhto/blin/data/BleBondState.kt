package com.grandfatherpikhto.blin.data

import com.grandfatherpikhto.blin.BleBondManager

data class BleBondState (val bleDevice: BleDevice, val state: BleBondManager.State)