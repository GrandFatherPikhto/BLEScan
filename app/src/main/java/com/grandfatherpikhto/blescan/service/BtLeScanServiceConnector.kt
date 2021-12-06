package com.grandfatherpikhto.blescan.service

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.grandfatherpikhto.blescan.model.BtLeDevice
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@DelicateCoroutinesApi
@InternalCoroutinesApi
object BtLeScanServiceConnector: ServiceConnection {
    const val TAG = "ScannerConnector"

    /** Геттер для сервиса */
    private var btLeScanService: BtLeScanService? = null
    val service: BtLeScanService? get() = btLeScanService

    private val _bound = MutableStateFlow(false)
    val bound: StateFlow<Boolean> = _bound

    private val _device = MutableSharedFlow<BtLeDevice?>(replay = 10)
    val device = _device.asSharedFlow()

    private val _state = MutableStateFlow<BtLeScanService.State>(BtLeScanService.State.Stopped)
    val state: SharedFlow<BtLeScanService.State> = _state

    override fun onServiceConnected(componentName: ComponentName?, binderService: IBinder?) {
        btLeScanService = (binderService as BtLeScanService.LocalBinder).getService()
        _bound.tryEmit(true)

        GlobalScope.launch {
            // _state = btLeScanService!!.state as MutableStateFlow<BtLeScanService.State>
            bound.collect { value ->
                if(value && btLeScanService != null) {
                    btLeScanService!!.state.collect { state ->
                        _state.tryEmit(state)
                    }
                }
            }
        }

        GlobalScope.launch {
            LeScanCallback.device.collect { item ->
                if (item != null) {
                    _device.emit(item)
                }
            }
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        Log.d(TAG, "onServiceDisconnected")
        GlobalScope.launch {
            _bound.tryEmit(false)
            btLeScanService = null
        }
    }

    override fun onNullBinding(name: ComponentName?) {
        super.onNullBinding(name)
        Log.d(TAG, "nullBinding")
        GlobalScope.launch {
            _bound.tryEmit(false)
            btLeScanService = null
        }
    }


    fun scanLeDevices(addresses: List<String> = listOf(),
                     names: List<String> = listOf<String>(),
                     mode:BtLeScanService.Mode = BtLeScanService.Mode.FindAll)
        = btLeScanService?.scanLeDevices(addressesList = addresses, namesList = names, modeScan = mode)

    fun scanLeDevices(addresses: String? = null, names: String? = null, mode:BtLeScanService.Mode = BtLeScanService.Mode.FindAll)
        = btLeScanService?.scanLeDevices(addresses, names, mode)

//    fun scanLeDevices(addresses: String? = null,
//            names:String? = null,
//            mode:BtLeScanService.Mode = BtLeScanService.Mode.FindAll) {
//        val addressesList = addresses?.let {
//            it.split(",")?.filter { address ->
//                address.trim().isNotEmpty()
//                && BluetoothAdapter.checkBluetoothAddress(address)
//            }
//        } ?: listOf()
//        val namesList = names?.let {
//            it.split(",")?.filter { name ->
//                name.trim().isNotEmpty() }
//        } ?: listOf()
//        btLeScanService?.scanLeDevices(addressesList = addressesList, namesList = namesList, modeScan = mode)
//    }


    fun stopScan() = btLeScanService?.stopScan()

    fun pairedDevices() = btLeScanService?.pairedDevices()

    fun disableBluetooth() = btLeScanService?.disableBluetooth()
}