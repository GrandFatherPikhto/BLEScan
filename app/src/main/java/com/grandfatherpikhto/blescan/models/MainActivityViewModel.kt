package com.grandfatherpikhto.blescan.models

import androidx.lifecycle.ViewModel
import com.grandfatherpikhto.blin.data.BleScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivityViewModel : ViewModel() {
    private val mutableStateFlowScanResult = MutableStateFlow<BleScanResult?>(null)
    val stateFlowScanResult get() = mutableStateFlowScanResult.asStateFlow()
    val scanResult get() = mutableStateFlowScanResult.value

    fun changeScanResult(bleScanResult: BleScanResult) {
        mutableStateFlowScanResult.tryEmit(bleScanResult)
    }
}