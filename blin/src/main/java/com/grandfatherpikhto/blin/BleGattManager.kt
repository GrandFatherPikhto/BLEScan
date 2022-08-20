package com.grandfatherpikhto.blin

import android.content.Context
import com.grandfatherpikhto.blin.orig.AbstractBleGattManager
import com.grandfatherpikhto.blin.orig.AbstractBleScanManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class BleGattManager (context: Context, bleScanManager: AbstractBleScanManager,
                      dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : AbstractBleGattManager(context, bleScanManager, dispatcher)