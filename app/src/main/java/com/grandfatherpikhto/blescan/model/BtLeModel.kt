package com.grandfatherpikhto.blescan.model

import android.bluetooth.BluetoothGatt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.blescan.ScanFragment
import com.grandfatherpikhto.blescan.service.BtLeService
import com.grandfatherpikhto.blescan.service.BtLeServiceConnector
import com.grandfatherpikhto.blescan.service.LeScanCallback
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@DelicateCoroutinesApi
@InternalCoroutinesApi
class BtLeModel: ViewModel() {
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
    private val _action = MutableLiveData<ScanFragment.Action>(ScanFragment.Action.None)
    val action:LiveData<ScanFragment.Action> = _action
    /** */
    private val devicesList = mutableListOf<BtLeDevice>()
    private val _devices = MutableLiveData<List<BtLeDevice>>(listOf<BtLeDevice>())
    val devices:LiveData<List<BtLeDevice>> = _devices
    /** */
    private val _bound = MutableLiveData<Boolean>(false)
    val bound:LiveData<Boolean> = _bound
    /** */
    private val _device = MutableLiveData<BtLeDevice?>(null)
    val device:LiveData<BtLeDevice?> = _device
    /** */
    private val _enabled = MutableLiveData<Boolean>(false)
    val enabled:LiveData<Boolean> = _enabled

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
        viewModelScope.launch {
            BtLeServiceConnector.bound.collect { bondValue ->
                _bound.postValue(bondValue)
                if(bondValue) {
                    BtLeServiceConnector.state.collect { stateValue ->
                        _state.postValue(stateValue)
                    }
                }
            }
        }

        viewModelScope.launch {
            BtLeServiceConnector.device.collect { finded ->
                _device.postValue(finded)
                if(finded != null) {
                    if (devicesList.find { it.address == finded!!.address } == null) {
                        devicesList.add(finded!!)
                        _devices.postValue(devicesList.toList())
                    }
                }
            }
        }

        viewModelScope.launch {
            BtLeServiceConnector.enabled.collect { enabled ->
                _enabled.postValue(enabled)
            }
        }
    }

    fun changeAction(value:ScanFragment.Action) {
        _action.postValue(value)
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun clean() {
        devicesList.clear()
        _devices.postValue(devicesList.toList())
    }

    fun changeAddress(bluetoothAddress: String) {
        _address.postValue(bluetoothAddress)
    }
}