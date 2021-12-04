package com.grandfatherpikhto.blescan.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.blescan.ScanFragment
import com.grandfatherpikhto.blescan.service.BtLeScanService
import com.grandfatherpikhto.blescan.service.BtLeScanServiceConnector
import com.grandfatherpikhto.blescan.service.LeScanCallback
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@DelicateCoroutinesApi
@InternalCoroutinesApi
class BtLeScanModel : ViewModel() {
    companion object {
        const val TAG:String = "BtLeScanModel"
    }

    /** */
    private val _action = MutableLiveData<ScanFragment.Action>(ScanFragment.Action.None)
    val action:LiveData<ScanFragment.Action> = _action
    /** */
    private val devicesList = mutableListOf<BtLeDevice>()
    private val _devices = MutableLiveData<List<BtLeDevice>>(listOf<BtLeDevice>())
    val devices:LiveData<List<BtLeDevice>> = _devices
    /** */
    private val _state = MutableLiveData<BtLeScanService.State>(BtLeScanService.State.Stopped)
    val state:LiveData<BtLeScanService.State> = _state
    /** */
    private val _bound = MutableLiveData<Boolean>(false)
    val bound:LiveData<Boolean> = _bound
    /** */
    private val _device = MutableLiveData<BtLeDevice?>(null)
    val device:LiveData<BtLeDevice?> = _device


    /** */
    init {
        viewModelScope.launch {
            BtLeScanServiceConnector.bound.collect { bondValue ->
                _bound.postValue(bondValue)
                if(bondValue) {
                    BtLeScanServiceConnector.state.collect { stateValue ->
                        _state.postValue(stateValue)
                    }
                }
            }
        }

        viewModelScope.launch {
            LeScanCallback.device.collect { finded ->
                _device.postValue(finded)
                if(finded != null) {
                    if (devicesList.find { it.address == finded!!.address } == null) {
                        devicesList.add(finded!!)
                        _devices.postValue(devicesList.toList())
                    }
                }
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
}