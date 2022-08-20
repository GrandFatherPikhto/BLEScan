package com.grandfatherpikhto.blescan.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.blin.orig.AbstractBleScanManager
import com.grandfatherpikhto.blescan.blemanager.AppBleManager
import com.grandfatherpikhto.blescan.data.BleScanResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScanViewModel() : ViewModel() {
    private val mutableSharedFlowScanResult     = MutableSharedFlow<BleScanResult>(replay = 100)
    private val mutableStateFLowScanState       = MutableStateFlow(AbstractBleScanManager.State.Stopped)
    private val mutableStateFlowScanError       = MutableStateFlow(-1)

    val sharedFlowScanResult     get() = mutableSharedFlowScanResult.asSharedFlow()
    val stateFlowScanState       get() = mutableStateFLowScanState.asStateFlow()
    val scanState                get() = mutableStateFLowScanState.value
    val stateFlowScanError       get() = mutableStateFlowScanError.asStateFlow()
    val scanError                get() = mutableStateFlowScanError.value

    fun changeBleManager(bleManager: AppBleManager) {
        viewModelScope.launch {
            bleManager.sharedFlowBleScanResult.collect {
                mutableSharedFlowScanResult.tryEmit(it)
            }
        }

        viewModelScope.launch {
            bleManager.stateFlowScanState.collect {
                mutableStateFLowScanState.tryEmit(it)
            }
        }

        viewModelScope.launch {
            bleManager.stateFlowScanError.collect {
                mutableStateFlowScanError.tryEmit(it)
            }
        }
    }
}