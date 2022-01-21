package com.grandfatherpikhto.blescan.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeServiceConnector: ServiceConnection {
    companion object {
        private const val TAG = "BtLeServiceConnector"
    }

    private var btLeService:BtLeService ?= null
    val service:BtLeService? get() = btLeService

    override fun onServiceConnected(p0: ComponentName?, serviceBinder: IBinder?) {
        btLeService = (serviceBinder as BtLeService.LocalBinder).getService()
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        btLeService = null
    }

    override fun onNullBinding(name: ComponentName?) {
        super.onNullBinding(name)
        btLeService = null
    }

    override fun onBindingDied(name: ComponentName?) {
        super.onBindingDied(name)
        btLeService = null
    }
}