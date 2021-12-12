package com.grandfatherpikhto.blescan.model

import android.bluetooth.BluetoothGatt
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.grandfatherpikhto.blescan.ScanFragment
import com.grandfatherpikhto.blescan.service.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@DelicateCoroutinesApi
@InternalCoroutinesApi
class BtLeModel: ViewModel() {
    /** */
    companion object {
        const val TAG:String = "BtLeDeviceModel"
    }
    /** */
    private val bluetoothInterface:BluetoothInterface by BluetoothInterfaceLazy()
    /** */
    private val _service = MutableLiveData<BtLeService?>(null)
    val service:LiveData<BtLeService?> get() = _service
    /** */
    private val _scanner = MutableLiveData<BtLeScanner.State>(BtLeScanner.State.Unknown)
    val scanner:LiveData<BtLeScanner.State> = _scanner
    /** */
    private val _connector = MutableLiveData<BtLeConnector.State>(BtLeConnector.State.Unknown)
    val connector:LiveData<BtLeConnector.State> = _connector
    /** */
    private val _gatt = MutableLiveData<BluetoothGatt?>(null)
    val gatt:LiveData<BluetoothGatt?> = _gatt
    /** */
    private val _address = MutableLiveData<String?>(null)
    val address:LiveData<String?> = _address
    /** */
    private val _action:MutableLiveData<ScanFragment.Action>
        = MutableLiveData(ScanFragment.Action.None)
    val action:LiveData<ScanFragment.Action> = _action
    /** */
    private val devicesList = mutableListOf<BtLeDevice>()
    private val _devices = MutableLiveData<List<BtLeDevice>>(listOf<BtLeDevice>())
    val devices:LiveData<List<BtLeDevice>> = _devices
    /** */
    private val _bond = MutableLiveData<Boolean>(false)
    val bond:LiveData<Boolean> = _bond
    /** */
    private val _device = MutableLiveData<BtLeDevice?>(null)
    val device:LiveData<BtLeDevice?> = _device
    /** */
    private val _enabled = MutableLiveData<Boolean>(false)
    val enabled:LiveData<Boolean> = _enabled

    private val bluetoothListener = object: BluetoothListener {
        override fun onBluetoothEnabled(enabled: Boolean) {
            super.onBluetoothEnabled(enabled)
            _enabled.postValue(enabled)
        }

        override fun onFindDevice(btLeDevice: BtLeDevice?) {
            super.onFindDevice(btLeDevice)
            _device.postValue(btLeDevice)
            btLeDevice?.let { found ->
                devicesList.add(found)
                _devices.postValue(devicesList)
            }
        }

        override fun onGattChanged(bluetoothGatt: BluetoothGatt?) {
            super.onGattChanged(bluetoothGatt)
            _gatt.postValue(bluetoothGatt)
        }

        override fun onServiceBound(oldValue: BtLeService?, newValue: BtLeService?) {
            super.onServiceBound(oldValue, newValue)
            Log.d(TAG, "Bond: $newValue")
            if(newValue == null) {
                _bond.postValue(false)
            } else {
                _bond.postValue(true)
            }
            _service.postValue(newValue)
        }

        override fun onChangeScannerState(
            oldState: BtLeScanner.State,
            newState: BtLeScanner.State
        ) {
            super.onChangeScannerState(oldState, newState)
            _scanner.postValue(newState)
        }

        override fun onChangeConnectorState(
            oldState: BtLeConnector.State,
            newState: BtLeConnector.State
        ) {
            super.onChangeConnectorState(oldState, newState)
            _connector.postValue(newState)
        }

        override fun onSetCurrentDevice(oldValue: BtLeDevice?, newValue: BtLeDevice?) {
            super.onSetCurrentDevice(oldValue, newValue)
            _address.postValue(newValue?.address)
        }
    }

    /** */
    init {
        bluetoothInterface.addListener(bluetoothListener)
    }

    fun changeAction(value:ScanFragment.Action) {
        Log.d(TAG, "Action: $value")
        _action.postValue(value)
    }

    fun clean() {
        devicesList.clear()
        _devices.postValue(devicesList.toList())
    }

    fun changeAddress(bluetoothAddress: String) {
        _address.postValue(bluetoothAddress)
        bluetoothInterface.currentDevice = BtLeDevice(address = bluetoothAddress)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothInterface.removeListener(bluetoothListener)
    }
}