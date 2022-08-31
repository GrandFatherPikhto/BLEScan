package com.grandfatherpikhto.blin.orig

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import com.grandfatherpikhto.blin.data.BleBondState
import com.grandfatherpikhto.blin.receivers.BcBondReceiver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class AbstractBleBondManager (private val context: Context,
                                       private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    private val logTag = this.javaClass.simpleName

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter
            = bluetoothManager.adapter
    private val applicationContext:Context get() = context.applicationContext

    private val scope = CoroutineScope(dispatcher)

    private var requestDevice: BluetoothDevice? = null

    enum class State (val value: Int) {
        None(0x00),
        Request(0x01),
        Bonding(0x02),
        Bonded(0x03),
        Reject(0x04),
        Error(0xFF)
    }

    private val mutableStateFlowBleBondState = MutableStateFlow<BleBondState?>(null)
    val stateFlowBondState get() = mutableStateFlowBleBondState.asStateFlow()
    val bondState get() = mutableStateFlowBleBondState.value

    private val bcBondReceiver by lazy {
        BcBondReceiver(this, dispatcher)
    }

    init {
        applicationContext.applicationContext.registerReceiver(bcBondReceiver,
            makeIntentFilter())
    }

    fun onDestroy() {
        applicationContext.unregisterReceiver(bcBondReceiver)
    }

    /**
     * Uppercase -- важно! Потому, что иначе, устройство не будет найдено!
     */
    @SuppressLint("MissingPermission")
    fun bondRequest(address: String) : Boolean {
        /**
         * https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#enable()
         */
        if (!bluetoothAdapter.isEnabled) {
            return false
        }

        val validAddress = address.uppercase()
        if (BluetoothAdapter.checkBluetoothAddress(validAddress)) {
            bluetoothAdapter.getRemoteDevice(address.uppercase())?.let { bluetoothDevice ->
                // println("bondRequest(${bluetoothDevice.address}, ${bluetoothDevice.name})")
                return bondRequest(bluetoothDevice)
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun bondRequest(bluetoothDevice: BluetoothDevice) : Boolean {
        /**
         * https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#enable()
         */
        if (!bluetoothAdapter.isEnabled) {
            return false
        }

        Log.d(logTag, "bondRequest(${bluetoothDevice.address})")
        if(bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED) {
            mutableStateFlowBleBondState.tryEmit(BleBondState(bluetoothDevice, State.Bonded))
        } else {
            requestDevice = bluetoothDevice
            if (bluetoothDevice.createBond()) {
                mutableStateFlowBleBondState.tryEmit(BleBondState(bluetoothDevice, State.Request))
                return true
            } else {
                mutableStateFlowBleBondState.tryEmit(BleBondState(bluetoothDevice, State.Error))
            }
        }

        return false
    }

    private fun makeIntentFilter() = IntentFilter().let { intentFilter ->
        intentFilter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)

        intentFilter
    }

    /**
     * BluetoothDevice.BOND_NONE    10
     * BluetoothDevice.BOND_BONDING 11
     * BluetoothDevice.BOND_BONDED  12
     */
    @SuppressLint("MissingPermission")
    fun onSetBondingDevice(bluetoothDevice: BluetoothDevice?, oldState: Int, newState: Int) {
        bluetoothDevice?.let { device ->
            if (device == requestDevice) {
                Log.d(logTag, "onSetBondingDevice($bluetoothDevice, $oldState, $newState)")
                when(newState) {
                    BluetoothDevice.BOND_BONDING -> { mutableStateFlowBleBondState
                        .tryEmit(BleBondState(bluetoothDevice, State.Bonding)) }
                    BluetoothDevice.BOND_BONDED -> {
                        bluetoothAdapter.bondedDevices.add(bluetoothDevice)
                        mutableStateFlowBleBondState
                        .tryEmit(BleBondState(bluetoothDevice, State.Bonded))
                    }
                    BluetoothDevice.BOND_NONE -> {
                        bluetoothAdapter.bondedDevices.remove(bluetoothDevice)
                        mutableStateFlowBleBondState
                        .tryEmit(BleBondState(bluetoothDevice, State.Reject))
                    }
                    else -> { Log.d(logTag, "Unknown State: $newState")}
                }
            }
        }
    }
}