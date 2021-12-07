package com.grandfatherpikhto.blescan.service

import android.bluetooth.BluetoothGatt
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
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
object BtLeServiceConnector:ServiceConnection {
    private const val TAG = "BtLeServiceConnector"
    private var btLeService:BtLeService ?= null
    val service:BtLeService? get() = btLeService

    private var sharedBound = MutableStateFlow<Boolean>(false)
    val bound: StateFlow<Boolean> = sharedBound

    private val sharedState = MutableStateFlow(BtLeService.State.Disconnected)
    val state: StateFlow<BtLeService.State> = sharedState

    private val sharedGatt = MutableStateFlow<BluetoothGatt?>(null)
    val gatt = sharedGatt.asStateFlow()

    private val sharedDevice = MutableSharedFlow<BtLeDevice?>(replay = 10)
    val device = sharedDevice.asSharedFlow()

    private val sharedEnabled = MutableStateFlow<Boolean>(false)
    val enabled = sharedEnabled.asStateFlow()


    override fun onServiceConnected(p0: ComponentName?, serviceBinder: IBinder?) {
        // Log.d(TAG, "Сервис подключён")
        btLeService = (serviceBinder as BtLeService.LocalBinder).getService()
        GlobalScope.launch {
            sharedBound.tryEmit(true)
            btLeService?.state?.collect { value ->
                sharedState.tryEmit(value)
            }
        }
        GlobalScope.launch {
            btLeService?.gatt?.collect { value ->
                sharedGatt.tryEmit(value)
            }
        }
        GlobalScope.launch {
            btLeService?.device?.collect{ found ->
                sharedDevice.tryEmit(found)
            }
        }
        GlobalScope.launch {
            btLeService?.enabled?.collect { enabled ->
                sharedEnabled.tryEmit(enabled)
            }
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        GlobalScope.launch {
            sharedBound.tryEmit(false)
        }
    }

    fun connect(address:String) = btLeService?.connect(address)
    fun connect(btLeDevice:BtLeDevice) = btLeService?.connect(btLeDevice)
    fun close() = btLeService?.close()

    fun scanLeDevices( addresses: String? = null,
                       names: String? = null,
                       mode: BtLeScanner.Mode = BtLeScanner.Mode.FindAll)
        = btLeService?.scanLeDevices(addresses, names, mode)

    fun scanLeDevices( addresses: Array<String> = arrayOf<String>(),
                       names: Array<String> = arrayOf<String>(),
                       mode: BtLeScanner.Mode = BtLeScanner.Mode.FindAll)
            = btLeService?.scanLeDevices(addresses, names, mode)

    fun stopScan() = btLeService?.stopScan()

    fun pairedDevices() = btLeService?.pairedDevices()

}