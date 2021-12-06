package com.grandfatherpikhto.blescan.service

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
object BtLeServiceConnector:ServiceConnection {
    private const val TAG = "BtLeServiceConnector"
    private var btLeService:BtLeService ?= null
    val service:BtLeService? get() = btLeService

    private var sharedBond = MutableStateFlow<Boolean>(false)
    val bond: StateFlow<Boolean> = sharedBond

    private val sharedState = MutableStateFlow(BtLeService.State.Disconnected)
    val state: StateFlow<BtLeService.State> = sharedState

    private val sharedGatt = MutableStateFlow<BluetoothGatt?>(null)
    val gatt = sharedGatt.asStateFlow()

    override fun onServiceConnected(p0: ComponentName?, serviceBinder: IBinder?) {
        // Log.d(TAG, "Сервис подключён")
        btLeService = (serviceBinder as BtLeService.LocalBinder).getService()
        GlobalScope.launch {
            sharedBond.tryEmit(true)
            btLeService?.state?.collect { value ->
                sharedState.tryEmit(value)
            }
        }
        GlobalScope.launch {
            btLeService?.gatt?.collect { value ->
                sharedGatt.tryEmit(value)
            }
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        GlobalScope.launch {
            sharedBond.tryEmit(false)
        }
    }

    fun connect(address:String) = btLeService?.connect(address)
    fun close() = btLeService?.close()
}