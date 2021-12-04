package com.grandfatherpikhto.blescan.service

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import com.grandfatherpikhto.blescan.helper.toBtLeDevice
import com.grandfatherpikhto.blescan.model.BtLeDevice
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

object LeScanCallback: ScanCallback() {
    const val TAG: String = "LeScanCallback"
    /** */
    private val addresses = mutableListOf<String>()
    /** */
    private val names     = mutableListOf<String>()
    /** */
    private val services  = mutableListOf<String>()
    /** */
    private val _device = MutableSharedFlow<BtLeDevice?> (replay = 10 )
    val device = _device.asSharedFlow()

    /** */
    private val _error = MutableStateFlow<Int>(0)
    val error: StateFlow<Int> = _error

    private fun checkName(bluetoothDevice: BluetoothDevice): Boolean {
        // Log.d(TAG, "checkName: ${names.size}")
        if(names.isNotEmpty()) {
            // Log.d(TAG, "checkName: ${names.contains(bluetoothDevice.name)}")
            if (bluetoothDevice.name == null) return false
            return names.contains(bluetoothDevice.name)
        }
        return true
    }

    private fun checkAddress(bluetoothDevice: BluetoothDevice): Boolean {
        // Log.d(TAG, "checkAddress: ${addresses.joinToString (", ")}, ${addresses.isNotEmpty()}")
        if(addresses.isNotEmpty()) {
            // Log.d(TAG, "Contains: ${addresses.contains(bluetoothDevice.address)}")
            return addresses.contains(bluetoothDevice.address)
        }
        return true
    }

    /**
     * TODO: Сделай опрос коннект, опрос сервисов. Видимо, придётся, создать очередь.
     */
    private fun checkServices(bluetoothDevice: BluetoothDevice): Boolean {
        if(services.size != 0) {
            return false
        }
        return true
    }


    private fun emitDevice(bluetoothDevice: BluetoothDevice?) {
        if(bluetoothDevice != null) {
            if(checkName(bluetoothDevice)
                &&  checkAddress(bluetoothDevice)) {
                // Log.d(TAG, "emitDevice [${bluetoothDevice.name}, ${bluetoothDevice.address}, checkName: ${checkName(bluetoothDevice)}, checkAddress: ${checkAddress(bluetoothDevice)}]")
                _device.tryEmit(bluetoothDevice.toBtLeDevice())
            }
        }
    }

    /**
     * Ошибка сканирования. Пока, никак не обрабатывается
     */
    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        _error.tryEmit(errorCode)
        Log.d(TAG, "Fail scan with error $errorCode")
    }

    /**
     * Пакетный режим (сразу несколько устройств)
     * Честно говоря, ни разу не видел, чтобы этот режим отрабатывал.
     */
    override fun onBatchScanResults(results: MutableList<ScanResult>?) {
        super.onBatchScanResults(results)
        results?.forEach { result ->
            // Log.d(TAG, "[BatchScan] Найдено устройство: ${result.device.address} ${result.device.name}")
            if(result?.device != null)
                emitDevice(result.device)
        }
    }

    /**
     * Найдено одно устройство.
     */
    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        super.onScanResult(callbackType, result)
        // Log.d(TAG, "[Scan] Найдено устройство: ${result?.device?.address} ${result?.device?.name}")
        if(result != null && result.device != null) {
            emitDevice(result.device)
        }
    }

    fun setAddresses(values: List<String>) {
        addresses.clear()
        addresses.addAll(values)
    }

    fun setNames(values: List<String>) {
        names.clear()
        names.addAll(values)
    }

    fun setServices(values: List<String>) {
        services.clear()
        services.addAll(values)
    }
}