package com.grandfatherpikhto.blescan.model

import android.bluetooth.BluetoothGatt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.grandfatherpikhto.blescan.service.BtLeService
import com.grandfatherpikhto.blescan.service.BtLeServiceConnector
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@DelicateCoroutinesApi
@InternalCoroutinesApi
class BtLeDeviceModel: ViewModel() {
    /** */
    companion object {
        const val TAG:String = "BtLeDeviceModel"
    }

    /** */
    private val _state = MutableLiveData<BtLeService.State>(BtLeService.State.Error)
    val state:LiveData<BtLeService.State> = _state

    /** */
    private val _gatt = MutableLiveData<BluetoothGatt?>(null)
    val gatt:LiveData<BluetoothGatt?> = _gatt

    private val _address = MutableLiveData<String?>(null)
    val address:LiveData<String?> = _address

    /** */
    init {
        GlobalScope.launch {
            BtLeServiceConnector.state.collect { value ->
                _state.postValue(value)
            }
        }

        GlobalScope.launch {
            BtLeServiceConnector.gatt.collect { value ->
                _gatt.postValue(value)
            }
        }
    }

    fun changeAddress(bluetoothAddress: String) {
        _address.postValue(bluetoothAddress)
    }
}