package com.grandfatherpikhto.blescan

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter
import android.util.Log
import com.grandfatherpikhto.blescan.service.BCReceiver

class BLEScanApp: Application() {
    companion object {
        const val TAG:String = "BleTestApp"
    }

    override fun onCreate() {
        super.onCreate()
        applicationContext.registerReceiver(BCReceiver, makeIntentFilter())

        Log.d(TAG, "OnCreate()")
    }

    override fun onTerminate() {
        applicationContext.unregisterReceiver(BCReceiver)
        super.onTerminate()
    }

    /**
     * Создаём фильтр перехвата для различных широковещательных событий
     * В данном случае, нужны только фильтры для перехвата
     * В данном случае, нужны только фильтры для перехвата
     * запроса на сопряжение устройства и завершения сопряжения
     * И интересует момент "Устройство найдено" на случай рескана устройств
     * по адресу или имени
     */
    private fun makeIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)

        return intentFilter
    }

}