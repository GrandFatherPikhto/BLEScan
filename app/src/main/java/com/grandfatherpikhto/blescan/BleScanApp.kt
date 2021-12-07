package com.grandfatherpikhto.blescan

import android.app.Application
import android.util.Log

class BleScanApp: Application() {
    companion object {
        const val TAG: String = "BleTestApp"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "OnCreate()")
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}