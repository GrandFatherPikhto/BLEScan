package com.grandfatherpikhto.blescan.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import com.grandfatherpikhto.blescan.helper.toBtLeDevice
import com.grandfatherpikhto.blescan.model.BtLeDevice
import com.grandfatherpikhto.blescan.model.BtLeScanModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class BtLeScanService: Service() {
    companion object {
        const val TAG:String = "BtLeScanService"
    }

    enum class State (val value: Int) {
        Stopped(0x0),
        Scanning(0x01)
    }

    enum class Mode(val value: Int) {
        FindAll(0x00),
        StopOnFind(0x01)
    }


    /** */
    private val _state = MutableStateFlow<State>(State.Stopped)
    val state = _state.asStateFlow()

    /** */
    private val _device = MutableSharedFlow<BtLeDevice?>(replay = 10)
    val device = _device.asSharedFlow()

    /** */
    private lateinit var bluetoothManager: BluetoothManager

    /** */
    private lateinit var bluetoothAdapter: BluetoothAdapter

    /** */
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    /** */
    private var bluetoothService: String ?= null

    /** */
    private var bluetoothAddress:String ?= null

    /** */
    private var bluetoothName:String ?= null

    /** */
    private var mode = Mode.FindAll

    /**
     * Класс, используемый для клиента Binder. Поскольку мы знаем, что эта служба всегда
     * выполняется в том же процессе, что и ее клиенты, нам не нужно иметь дело с IPC.
     */
    inner class LocalBinder : Binder() {
        /** Возвращает экземпляр LocalService, чтобы можно было использовать общедоступные методы */
        fun getService(): BtLeScanService = this@BtLeScanService
    }

    /** Binder given to clients */
    private val binder = LocalBinder()

    /**
     * Привязывание сервиса "штатным" BindService
     * Вызывается, когда клиент (MainActivity в случае этого приложения) выходит на передний план
     * и связывается с этой службой. Когда это произойдет, служба должна перестать быть службой
     * переднего плана.
     */
    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind()")
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        bluetoothManager   = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter   = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        GlobalScope.launch {
            LeScanCallback.device.collect { value ->
                // Log.d(TAG, "device: $device, $mode")
                if(mode == Mode.StopOnFind) {
                    stopScan()
                }
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind()")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    fun scanLeDevices(addressesList:List<String> = listOf()
                      , namesList:List<String> = listOf()
                      , modeScan: Mode = Mode.FindAll
    ) {
        Log.d(TAG, "scanLeDevices Address filter: ${addressesList.joinToString(", ")}, Name filter: ${namesList.joinToString(", ")}, Mode: ${modeScan}")
        LeScanCallback.setAddresses(addressesList)
        LeScanCallback.setNames(namesList)

        this.mode = modeScan

        startScan()
    }

    private fun startScan() {
        // stopScan()
        val scanFilters:MutableList<ScanFilter> = mutableListOf()

        // ScanSettings.MATCH_MODE_AGGRESSIVE
        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
//            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()
        // Log.d(TAG, "Filters: $scanFilters")
//        scanFilters.add(
//            ScanFilter.Builder().setDeviceAddress("7E:FC:58:23:24:DB").build()
//        )
        bluetoothLeScanner.startScan(scanFilters, scanSettings, LeScanCallback)
        _state.tryEmit(State.Scanning)
    }

    fun stopScan() {
        if(state.value == State.Scanning) {
            bluetoothLeScanner.stopScan(LeScanCallback)
            bluetoothLeScanner.flushPendingScanResults(LeScanCallback)
            _state.tryEmit(State.Stopped)
        }
    }

    fun pairedDevices() {
        Log.d(TAG, "Сопряжённые устройства")
        bluetoothAdapter.bondedDevices.forEach { device ->
            GlobalScope.launch {
                _device.tryEmit(device.toBtLeDevice())
                Log.d(TAG, "Сопряжённое устройство $device")
            }
        }
    }
}