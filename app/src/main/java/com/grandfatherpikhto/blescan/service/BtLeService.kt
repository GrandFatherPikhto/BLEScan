package com.grandfatherpikhto.blescan.service

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.grandfatherpikhto.blin.BtLeInterface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeService: Service() {
    /** */
    companion object {
        const val TAG:String = "BtLeService"
    }

    var btLeInterface: BtLeInterface? = null

    /** Binder given to clients */
    private val binder = LocalBinder()
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        fun getService(): BtLeService = this@BtLeService
    }

    /**
     *
     */
    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG, "Сервис связан")

        return binder
    }

    /**
     *
     */
    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    init {
        Log.d(TAG, "Init")
    }

    /**
     * TODO: Почему создание сервиса вызывется дважды/трижды?!
     * https://stackoverflow.com/questions/7211066/android-service-oncreate-is-called-multiple-times-without-calling-ondestroy
     */
    override fun onCreate() {
        super.onCreate()
        btLeInterface = BtLeInterface(applicationContext)
        Log.d(TAG, "onCreate()")
    }

    /**
     *
     */
    override fun onDestroy() {
        super.onDestroy()
        btLeInterface?.destroy()
        Log.d(TAG, "onDestroy()")
    }
}


