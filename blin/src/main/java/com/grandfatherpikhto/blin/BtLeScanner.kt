package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeScanner(private val btLeInterface: BtLeInterface) {
    companion object {
        const val TAG:String = "BtLeScanService"
    }

    enum class State (val value: Int) {
        Unknown(0x0),
        Stopped(0x1),
        Scanning(0x02),
        Error(0xFF)
    }

    interface ScannerCallback {
        fun onChangeState(oldState: State, newState: State) {}
        fun onFindDevice(btLeDevice: BluetoothDevice?) {}
        fun onScanError(error: Int) {}
    }

    enum class Mode(val value: Int) {
        FindAll(0x00),
        StopOnFind(0x01)
    }

    /** */
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()
    /** */
    private lateinit var bluetoothLeScanner:BluetoothLeScanner

    /** */
    private var leScanCallback: LeScanCallback? = null
    /** */
    private var mode = Mode.FindAll
    /**
     * Если установлен режим Mode.StopOnFind, процесс сканирования останавливается
     */
    private val bluetoothListener: BluetoothListener = object: BluetoothListener {
        override fun onFindDevice(btLeDevice: BluetoothDevice?) {
            super.onFindDevice(btLeDevice)
            if(mode == Mode.StopOnFind) {
                stopScan()
            }
        }
    }

    init {
        bluetoothLeScanner = bluetoothInterface.bluetoothAdapter!!.bluetoothLeScanner
        leScanCallback = LeScanCallback(btLeInterface)
        bluetoothInterface.addListener(bluetoothListener)
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
                      mode: Mode = Mode.FindAll
    ) {
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
        bluetoothInterface.scannerState = State.Scanning
    }

    fun stopScan() {
        if(bluetoothInterface.scannerState == State.Scanning) {
            bluetoothLeScanner.stopScan(leScanCallback)
            bluetoothLeScanner.flushPendingScanResults(leScanCallback)
            bluetoothInterface.scannerState = State.Stopped
        }
    }

    fun pairedDevices() {
        bluetoothInterface.bluetoothAdapter?.bondedDevices?.forEach { device ->
            bluetoothInterface.deviceFound = device
        }
    }

    fun destroy() {
        leScanCallback?.destroy()
        bluetoothInterface.removeListener(bluetoothListener)
    }
}