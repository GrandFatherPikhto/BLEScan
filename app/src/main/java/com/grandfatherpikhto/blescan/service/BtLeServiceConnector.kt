package com.grandfatherpikhto.blescan.service

import android.bluetooth.BluetoothGatt
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.grandfatherpikhto.blescan.model.BtLeDevice
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeServiceConnector: ServiceConnection {
    companion object {
        private const val TAG = "BtLeServiceConnector"
    }

    private var btLeService:BtLeService ?= null
    val service:BtLeService? get() = btLeService

    /** */
    private val bluetoothInterface:BluetoothInterface by BluetoothInterfaceLazy()

    override fun onServiceConnected(p0: ComponentName?, serviceBinder: IBinder?) {
        Log.d(TAG, "Сервис подключён")
        btLeService = (serviceBinder as BtLeService.LocalBinder).getService()
        bluetoothInterface.service = btLeService
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        bluetoothInterface.service = null
    }

    override fun onNullBinding(name: ComponentName?) {
        super.onNullBinding(name)
        bluetoothInterface.service = null
    }

    override fun onBindingDied(name: ComponentName?) {
        super.onBindingDied(name)
        bluetoothInterface.service = null
    }
}