package com.grandfatherpikhto.blescan

import android.app.Application
import android.util.Log

class BLEScanApp: Application() {
    companion object {
        const val TAG:String = "BleTestApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OnCreate()")
    }
}