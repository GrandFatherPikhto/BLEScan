package com.grandfatherpikhto.blin

import com.grandfatherpikhto.blin.orig.AbstractBleGattCallback
import com.grandfatherpikhto.blin.orig.AbstractBleGattManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class BleGattCallback (bleGattManager: AbstractBleGattManager,
                       dispatcher: CoroutineDispatcher = Dispatchers.IO) :
    AbstractBleGattCallback(bleGattManager, dispatcher)