package com.grandfatherpikhto.blescan.blemanager

import android.content.Context
import com.grandfatherpikhto.blin.orig.AbstractBleManager
import com.grandfatherpikhto.blescan.data.BleGatt
import com.grandfatherpikhto.blescan.data.BleScanResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class AppBleManager(context: Context) :
    AbstractBleManager(context) {

    private val mutableStateFlowBleGatt = MutableStateFlow<BleGatt?>(null)
    val stateFlowBleGatt = mutableStateFlowBleGatt.asStateFlow()
    val bleGatt get() = mutableStateFlowBleGatt.value

    val mutableSharedFlowBleScanResult = MutableSharedFlow<BleScanResult>(replay = 100)
    val sharedFlowBleScanResult get() = mutableSharedFlowBleScanResult.asSharedFlow()

    val bleScanResults by lazy { scanResults.map { BleScanResult(it) } }

    init {
        scope.launch {
            stateFlowBluetoothGatt.collect { bluetoothGatt ->
                if (bluetoothGatt == null) {
                    mutableStateFlowBleGatt.tryEmit(null)
                } else {
                    mutableStateFlowBleGatt.tryEmit(BleGatt(bluetoothGatt))
                }
            }
        }

        scope.launch {
            sharedFlowScanResults.collect {
                mutableSharedFlowBleScanResult.tryEmit(BleScanResult(it))
            }
        }
    }

    fun changeBleGatt(bleGatt: BleGatt?) {
        mutableStateFlowBleGatt.tryEmit(bleGatt)
    }

    open fun connectBle(address: String): BleGatt? {
        super.connect(address)?.let { bluetoothGatt ->
            return BleGatt(bluetoothGatt)
        }
        return null
    }
}