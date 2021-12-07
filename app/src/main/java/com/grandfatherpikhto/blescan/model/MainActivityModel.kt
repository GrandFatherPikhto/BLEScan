package com.grandfatherpikhto.blescan.model

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.grandfatherpikhto.blescan.MainActivity
import com.grandfatherpikhto.blescan.service.BcReceiver
import com.grandfatherpikhto.blescan.service.BtLeServiceConnector
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
            BtLeServiceConnector.enabled.collect { enabled ->
                Log.d(TAG, "State: $enabled")
                _enabled.postValue(enabled)
            }
        }
    }
}