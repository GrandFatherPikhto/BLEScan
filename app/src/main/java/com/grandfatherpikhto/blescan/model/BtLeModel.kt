package com.grandfatherpikhto.blescan.model

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.grandfatherpikhto.blescan.ScanFragment
import com.grandfatherpikhto.blescan.service.*
import com.grandfatherpikhto.blin.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@DelicateCoroutinesApi
@InternalCoroutinesApi
class BtLeModel: ViewModel() {
    /** */
    companion object {
        const val TAG:String = "BtLeModel"
    }
    /** */
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()
    /** */
    private val _scanner = MutableLiveData(BtLeScanner.State.Unknown)
    val scanner:LiveData<BtLeScanner.State> = _scanner
    /** */
    private val _connector = MutableLiveData(BtLeConnector.State.Unknown)
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
    private val devicesList = mutableListOf<BluetoothDevice>()
    private val _devices = MutableLiveData(listOf<BluetoothDevice>())
    val devices:LiveData<List<BluetoothDevice>> = _devices
    /** */
    private val _bond = MutableLiveData(false)
    val bond:LiveData<Boolean> = _bond
    /** */
    private val _device = MutableLiveData<BluetoothDevice?>(null)
    val device:LiveData<BluetoothDevice?> = _device
    /** */
    private val _enabled = MutableLiveData(false)
    val enabled:LiveData<Boolean> = _enabled

    private val bluetoothListener = object: BluetoothListener {
        override fun onBluetoothEnabled(enabled: Boolean) {
            super.onBluetoothEnabled(enabled)
            _enabled.postValue(enabled)
        }

        override fun onFindDevice(btLeDevice: BluetoothDevice?) {
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

        override fun onSetCurrentDevice(oldValue: BluetoothDevice?, newValue: BluetoothDevice?) {
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

    override fun onCleared() {
        super.onCleared()
        bluetoothInterface.removeListener(bluetoothListener)
    }
}