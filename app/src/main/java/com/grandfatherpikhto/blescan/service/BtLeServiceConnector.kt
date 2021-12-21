package com.grandfatherpikhto.blescan.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.grandfatherpikhto.blin.BluetoothInterface
import com.grandfatherpikhto.blin.BluetoothInterfaceLazy
import com.grandfatherpikhto.blin.BtLeInterface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeServiceConnector: ServiceConnection {
    companion object {
        private const val TAG = "BtLeServiceConnector"
    }

    private var btLeService:BtLeService ?= null
    val service:BtLeService? get() = btLeService
    private var _btLeInterface: BtLeInterface? = null
    val btLeInterface get() = _btLeInterface

    /** */
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()

    override fun onServiceConnected(p0: ComponentName?, serviceBinder: IBinder?) {
        Log.d(TAG, "Сервис подключён")
        btLeService = (serviceBinder as BtLeService.LocalBinder).getService()
        btLeService?.let { service ->
            _btLeInterface = service.btLeInterface
        }
        // bluetoothInterface.btLeInterface = btLeService
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        // bluetoothInterface.btLeInterface = null
    }

    override fun onNullBinding(name: ComponentName?) {
        super.onNullBinding(name)
        // bluetoothInterface.btLeInterface = null
    }

    override fun onBindingDied(name: ComponentName?) {
        super.onBindingDied(name)
        // bluetoothInterface.btLeInterface = null
    }
}