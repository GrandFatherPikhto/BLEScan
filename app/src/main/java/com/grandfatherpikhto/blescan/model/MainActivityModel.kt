package com.grandfatherpikhto.blescan.model

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.grandfatherpikhto.blescan.MainActivity
import com.grandfatherpikhto.blescan.service.*
import com.grandfatherpikhto.blin.BluetoothInterface
import com.grandfatherpikhto.blin.BluetoothInterfaceLazy
import com.grandfatherpikhto.blin.BluetoothListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@DelicateCoroutinesApi
class MainActivityModel: ViewModel() {
    companion object {
        const val TAG:String = "MainViewModel"
    }

    /** */
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()
    /** */
    private val bluetoothListener = object: BluetoothListener {
        override fun onBluetoothEnabled(enabled: Boolean) {
            super.onBluetoothEnabled(enabled)
            _enabled.postValue(enabled)
        }
    }

    private val _bond = MutableLiveData<Boolean>(false)
    val bond:LiveData<Boolean> get() = _bond

    private val _device = MutableLiveData<BluetoothDevice?>(null)
    val device:LiveData<BluetoothDevice?> get() = _device

    private val _ready = MutableLiveData<Boolean>(true)
    val ready:LiveData<Boolean> get() = _ready

    private val _enabled = MutableLiveData<Boolean>(false)
    val enabled:LiveData<Boolean> get() = _enabled

    private val _current = MutableLiveData<MainActivity.Current>(MainActivity.Current.Scanner)
    val current:LiveData<MainActivity.Current> get() = _current

    fun changeDevice(value: BluetoothDevice) {
        bluetoothInterface.currentDevice = value
        _device.postValue(value)
    }

    fun changeCurrent(value: MainActivity.Current) {
        _current.postValue(value)
    }

    fun changeReady(value: Boolean) {
        _ready.postValue(value)
    }

    fun andReady(value: Boolean) {
        _ready.value?.let {
            _ready.postValue(_ready.value!!.and(value))
        }
    }

    fun changeEnabled(value: Boolean) {
        _enabled.postValue(value)
    }

    init {
        bluetoothInterface.addListener(bluetoothListener)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothInterface.removeListener(bluetoothListener)
    }
}