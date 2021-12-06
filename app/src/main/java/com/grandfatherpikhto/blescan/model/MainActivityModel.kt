package com.grandfatherpikhto.blescan.model

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.grandfatherpikhto.blescan.MainActivity
import com.grandfatherpikhto.blescan.service.BCReceiver
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@InternalCoroutinesApi
@DelicateCoroutinesApi
class MainActivityModel: ViewModel() {
    companion object {
        const val TAG:String = "MainViewModel"
    }

    private val _device = MutableLiveData<BtLeDevice?>(null)
    val device:LiveData<BtLeDevice?> = _device

    private val _ready = MutableLiveData<Boolean>(true)
    val ready:LiveData<Boolean> = _ready

    private val _enabled = MutableLiveData<Boolean>(false)
    val enabled:LiveData<Boolean> = _enabled

    private val _current = MutableLiveData<MainActivity.Current>(MainActivity.Current.Scanner)
    val current:LiveData<MainActivity.Current> = _current

    fun changeDevice(value: BtLeDevice) {
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
        GlobalScope.launch {
            BCReceiver.btState.collect { state ->
                Log.d(TAG, "State: $state")
                when(state) {
                    BluetoothAdapter.STATE_ON -> {
                        if(_enabled.value == false) {
                            _enabled.postValue(true)
                        }
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        if(_enabled.value == true) {
                            _enabled.postValue(false)
                        }
                    }
                }
            }
        }
    }
}