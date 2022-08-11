package com.grandfatherpikhto.blin.idling

import androidx.test.espresso.IdlingResource
import com.grandfatherpikhto.blin.BleManagerInterface
import com.grandfatherpikhto.blin.BleScanManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

class ScanIdling (private val bleManager: BleManagerInterface) : IdlingResource {
    companion object {
        private var scanIdling:ScanIdling? = null
        fun getInstance(bleManager: BleManagerInterface) : ScanIdling {
            return scanIdling ?: ScanIdling(bleManager)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private var resourceCallback: IdlingResource.ResourceCallback? = null

    private var isIdling = AtomicBoolean(false)

    var idling by Delegates.observable(false) { _, _, newState ->
        isIdling.set(newState)
        if (newState) {
            resourceCallback?.let { callback ->
                callback.onTransitionToIdle()
            }
        }
    }

    init {
        scope.launch {
            bleManager.stateFlowScanState.collect { state ->
                when(state) {
                    BleScanManager.State.Stopped -> {
                        idling = true
                    }
                    BleScanManager.State.Scanning -> {
                        idling = false
                    }
                    else -> { }
                }
            }
        }
    }

    override fun getName(): String = this.javaClass.simpleName

    override fun isIdleNow(): Boolean = isIdling.get()
    // override fun isIdleNow(): Boolean = idling

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        resourceCallback = callback
    }
}