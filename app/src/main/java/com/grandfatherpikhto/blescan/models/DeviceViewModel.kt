package com.grandfatherpikhto.blescan.models

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.blin.BleGattManager
import com.grandfatherpikhto.blin.BleManagerInterface
import com.grandfatherpikhto.blin.data.BleBondState
import com.grandfatherpikhto.blin.data.BleGatt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceViewModel: ViewModel () {

    private val logTag = this.javaClass.simpleName

    private val mutableStateFlowConnectState = MutableStateFlow(BleGattManager.State.Disconnected)
    val stateFlowConnectState get() = mutableStateFlowConnectState.asStateFlow()
    val connectState          get() = mutableStateFlowConnectState.value

    private val mutableStateFlowStateCode = MutableStateFlow(-1)
    val stateFlowConnectStateCode get() = mutableStateFlowStateCode.asStateFlow()
    val stateCode                 get() = mutableStateFlowStateCode.value

    private val mutableStateFlowBleGatt = MutableStateFlow<BleGatt?>(null)
    val stateFlowGatt get() = mutableStateFlowBleGatt.asStateFlow()
    val bluetoothGatt get() = mutableStateFlowBleGatt.value

    private val mutableStateFlowBondState = MutableStateFlow<BleBondState?>(null)
    val stateFlowBondState get() = mutableStateFlowBondState.asStateFlow()
    val bondState          get() = mutableStateFlowBondState.value

    private val mutableSharedFlowCharacteristic
        = MutableSharedFlow<BluetoothGattCharacteristic>(replay = 100)
    val sharedFlowCharacteristic get()
        = mutableSharedFlowCharacteristic.asSharedFlow()

    private val mutableSharedFlowDescriptor get()
        = MutableSharedFlow<BluetoothGattDescriptor>(replay = 100)
    val sharedFlowDescriptor get() = mutableSharedFlowDescriptor.asSharedFlow()

    var connected = false

    fun changeBleManager(bleManager: BleManagerInterface) {
        viewModelScope.launch {
            bleManager.stateFlowConnectState.collect {
                mutableStateFlowConnectState.tryEmit(it)
            }
        }

        viewModelScope.launch {
            bleManager.sharedFlowConnectStateCode.collect {
                mutableStateFlowStateCode.tryEmit(it)
            }
        }

        viewModelScope.launch {
            bleManager.stateFlowBleGatt.collect {
                mutableStateFlowBleGatt.tryEmit(it)
            }
        }

        viewModelScope.launch {
            bleManager.stateFlowBondState.collect {
                mutableStateFlowBondState.tryEmit(it)
            }
        }

        viewModelScope.launch {
            bleManager.sharedFlowCharacteristic.collect {
                mutableSharedFlowCharacteristic.tryEmit(it)
            }
        }

        viewModelScope.launch {
            bleManager.sharedFlowDescriptor.collect {
                mutableSharedFlowDescriptor.tryEmit(it)
            }
        }
    }

    override fun onCleared() {
        Log.d(logTag, "onCleared()")
        super.onCleared()
    }
}