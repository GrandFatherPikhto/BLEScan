package com.grandfatherpikhto.blin

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.grandfatherpikhto.blin.data.BleBondState
import com.grandfatherpikhto.blin.data.BleDevice
import com.grandfatherpikhto.blin.receivers.BcBondReceiver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleBondManager (private val bleManager: BleManager,
                      private val dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : DefaultLifecycleObserver {

    private val logTag = this.javaClass.simpleName

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

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        bleManager.applicationContext.applicationContext.registerReceiver(bcBondReceiver,
            makeIntentFilter())
    }

    override fun onDestroy(owner: LifecycleOwner) {
        bleManager.applicationContext.unregisterReceiver(bcBondReceiver)
        super.onDestroy(owner)
    }

    /**
     * Uppercase -- важно! Потому, что иначе, устройство не будет найдено!
     */
    fun bondRequest(address: String) : Boolean {
        (bleManager.applicationContext
            .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter.getRemoteDevice(address.uppercase())?.let { bluetoothDevice ->
                return bondRequest(bluetoothDevice)
            }

        return false
    }

    @SuppressLint("MissingPermission")
    fun bondRequest(bluetoothDevice: BluetoothDevice) : Boolean {
        println("Bond Device: ${bluetoothDevice.address}")
        if(bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED) {
            mutableStateFlowBleBondState.tryEmit(BleBondState(BleDevice(bluetoothDevice), State.Bonded))
        } else {
            requestDevice = bluetoothDevice
            if (bluetoothDevice.createBond()) {
                mutableStateFlowBleBondState.tryEmit(BleBondState(BleDevice(bluetoothDevice), State.Request))
                return true
            } else {
                mutableStateFlowBleBondState.tryEmit(BleBondState(BleDevice(bluetoothDevice), State.Error))
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
    fun onSetBondingDevice(bluetoothDevice: BluetoothDevice?, oldState: Int, newState: Int) {
        bluetoothDevice?.let { device ->
            if (device == requestDevice) {
                Log.d(logTag, "onSetBondingDevice($bluetoothDevice, $oldState, $newState)")
                when(newState) {
                    BluetoothDevice.BOND_BONDING -> { mutableStateFlowBleBondState
                        .tryEmit(BleBondState(BleDevice(bluetoothDevice), State.Bonding)) }
                    BluetoothDevice.BOND_BONDED -> { mutableStateFlowBleBondState
                        .tryEmit(BleBondState(BleDevice(bluetoothDevice), State.Bonded)) }
                    BluetoothDevice.BOND_NONE -> { mutableStateFlowBleBondState
                        .tryEmit(BleBondState(BleDevice(bluetoothDevice), State.Reject)) }
                    else -> { Log.d(logTag, "Unknown State: $newState")}
                }
            }
        }
    }
}