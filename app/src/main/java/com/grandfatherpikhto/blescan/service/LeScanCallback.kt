package com.grandfatherpikhto.blescan.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import com.grandfatherpikhto.blescan.helper.toBtLeDevice
import com.grandfatherpikhto.blescan.model.BtLeDevice
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@DelicateCoroutinesApi
@InternalCoroutinesApi
class LeScanCallback(service: BtLeService): ScanCallback() {
    companion object {
        const val TAG: String = "LeScanCallback"
    }

    interface LeScannerCallback {
        fun onFindDevice(btLeDevice: BtLeDevice)
        fun onError(error: Int)
    }

    /** */
    private var scannerCallbacks: MutableList<LeScannerCallback> = mutableListOf()
    /** */
    private val addresses = mutableListOf<String>()
    /** */
    private val names     = mutableListOf<String>()

    /** */
    private val _error = MutableStateFlow<Int>(0)
    val error: StateFlow<Int> = _error


    private fun checkName(bluetoothDevice: BluetoothDevice): Boolean {
        // Log.d(TAG, "checkName: ${names.size}")
        if(names.isNotEmpty()) {
            Log.d(TAG, "checkName: ${names.contains(bluetoothDevice.name)}")
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

    private fun emitDevice(bluetoothDevice: BluetoothDevice?) {
        if(bluetoothDevice != null) {
            // Log.d(TAG, "emitDevice [${bluetoothDevice.name}, ${bluetoothDevice.address}, checkName: ${checkName(bluetoothDevice)}, checkAddress: ${checkAddress(bluetoothDevice)}]")
            if(checkName(bluetoothDevice)
                &&  checkAddress(bluetoothDevice)) {
                scannerCallbacks.forEach { callback ->
                    callback.onFindDevice(bluetoothDevice.toBtLeDevice())
                }
            }
        }
    }

    /**
     * Ошибка сканирования. Пока, никак не обрабатывается
     */
    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)
        scannerCallbacks.forEach { callback ->
            callback.onError(errorCode)
        }
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

    fun setAddresses(values: Array<String>) {
        addresses.clear()
        addresses.addAll(values.filter { address ->
            address.trim().isNotBlank()
        })
    }

    fun setAddresses(value: String? = null) {
        addresses.clear()
        value?.trim()?.let {
            if (it.trim().isNotEmpty()) {
                addresses.addAll(it.split(",")?.filter { address ->
                    address.trim().isNotEmpty()
                            && BluetoothAdapter.checkBluetoothAddress(address)
                })
            }
        }
    }

    fun setNames(values: Array<String>) {
        names.clear()
        names.addAll(values.filter { name ->
            name.trim().isNotBlank()
        })
    }

    fun setNames(value:String? = null) {
        names.clear()
        value?.trim()?.let {
            if (it.isNotEmpty()) {
                it.let {
                    names.addAll(it?.split(",")?.filter { name ->
                        name.trim().isNotBlank()
                    })
                }
            }
        }
    }

    fun setOnEventListener(callback: LeScannerCallback) {
        scannerCallbacks.add(callback)
    }
}