package com.grandfatherpikhto.blescan.service

import android.bluetooth.le.BluetoothLeScanner
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.util.Log
import com.grandfatherpikhto.blescan.helper.toBtLeDevice
import com.grandfatherpikhto.blescan.model.BtLeDevice
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeScanner(private val service: BtLeService) {

    companion object {
        const val TAG:String = "BtLeScanService"
    }

    interface ScannerCallback {
        fun onChangeState(state:State) {}
        fun onFindDevice(btLeDevice: BtLeDevice?) {}
        fun onScanError(error: Int) {}
    }

    enum class State (val value: Int) {
        Stopped(0x0),
        Scanning(0x01),
        Error(0xFF)
    }

    enum class Mode(val value: Int) {
        FindAll(0x00),
        StopOnFind(0x01)
    }

    private var state:State = State.Stopped
    private var device:BtLeDevice? = null

    /** */
    private val scannerCallbacks:MutableList<ScannerCallback> = mutableListOf()

    /** */
    private val bluetoothLeScanner:BluetoothLeScanner by lazy {
        service.adapter.bluetoothLeScanner
    }

    /** */
    private var leScanCallback: LeScanCallback? = null

    /** */
    private var mode = Mode.FindAll

    init {
        leScanCallback = LeScanCallback(service)
        leScanCallback!!.setOnEventListener(object: LeScanCallback.LeScannerCallback {
            override fun onFindDevice(btLeDevice: BtLeDevice) {
                if(mode == Mode.StopOnFind) {
                    stopScan()
                }
                scannerCallbacks.forEach { callback ->
                    callback.onFindDevice(btLeDevice)
                }
            }

            override fun onError(error: Int) {
                scannerCallbacks.forEach { callback ->
                    callback.onScanError(error)
                }
            }
        })
    }

    fun scanLeDevices(addressesList:Array<String> = arrayOf()
                      , namesList:Array<String> = arrayOf()
                      , modeScan: Mode = Mode.FindAll
    ) {
        leScanCallback?.setAddresses(addressesList)
        leScanCallback?.setNames(namesList)

        this.mode = modeScan

        startScan()
    }

    fun scanLeDevices(addresses: String? = null,
                      names: String? = null,
                      mode: Mode = Mode.FindAll) {
        leScanCallback?.setAddresses(addresses)
        leScanCallback?.setNames(names)

        this.mode = mode

        startScan()
    }

    private fun startScan() {
        val scanFilters:MutableList<ScanFilter> = mutableListOf()

        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()
        bluetoothLeScanner.startScan(scanFilters, scanSettings, leScanCallback)
        state = State.Scanning
        scannerCallbacks.forEach { callback ->
            callback.onChangeState(State.Scanning)
        }
    }

    fun stopScan() {
        if(state == State.Scanning) {
            bluetoothLeScanner.stopScan(leScanCallback)
            bluetoothLeScanner.flushPendingScanResults(leScanCallback)
            state = State.Stopped
            scannerCallbacks.forEach { callback ->
                callback.onChangeState(state)
            }
        }
    }

    fun pairedDevices() {
        service.adapter.bondedDevices.forEach { found ->
            GlobalScope.launch {
                device = found.toBtLeDevice()
                scannerCallbacks.forEach { callback ->
                    callback.onFindDevice(found?.toBtLeDevice())
                }
            }
        }
    }

    fun addEventListener(callback: ScannerCallback) {
        scannerCallbacks.add(callback)
    }
}