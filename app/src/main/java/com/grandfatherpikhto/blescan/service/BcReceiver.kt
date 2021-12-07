package com.grandfatherpikhto.blescan.service

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.grandfatherpikhto.blescan.helper.toBtLeDevice
import com.grandfatherpikhto.blescan.model.BtLeDevice
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@InternalCoroutinesApi
@DelicateCoroutinesApi
@RequiresApi(Build.VERSION_CODES.M)
class BcReceiver(private val service: BtLeService) : BroadcastReceiver() {
    companion object {
        const val TAG:String = "BCReceiver"
    }

    private var bondingDevice:BluetoothDevice? = null

    private val _paired = MutableStateFlow<BluetoothDevice?>(null)
    val paired = _paired.asSharedFlow()

    private val _bt_state = MutableStateFlow<Int>(service.adapter.state)
    val btState = _bt_state.asStateFlow()

    interface ReceiverCallback {
        fun onBluetoothChangeState(state: Int) {}
        fun onBluetoothEnable(enable: Boolean) {}
        fun onBluetoothPaired(btLeDevice: BtLeDevice) {}
    }

    private var receiverCallbacks: MutableList<ReceiverCallback> = mutableListOf()


    /**
     * Проблема в том, что ACTION_BOND_STATE_CHANGED вызывается не только
     * после сопряжения устройства, но и при каждом подключении
     * Поэтому, при запросе на сопряжение сначала записываем устройство
     * в переменную bondingDevice.
     * Далее, когда приходит событие ACTION_BOND_STATE_CHANGED проверяем
     * был ли перед этим запрос на сопряжение. Если был, генерируется событие
     * paired. По этому событию вызывается попытка подключения.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent != null) {
            when(intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state    = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    // Log.d(TAG, "ACTION_STATE_CHANGED($state) ${state == BluetoothAdapter.STATE_ON}")
                    receiverCallbacks.forEach { callback ->
                        callback.onBluetoothChangeState(state)
                        when(state) {
                            BluetoothAdapter.STATE_ON  -> callback.onBluetoothEnable(true)
                            BluetoothAdapter.STATE_OFF -> callback.onBluetoothEnable(false)
                            else -> {}
                        }
                    }
                }
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    bondingDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    Log.d(TAG, "ACTION_PAIRING_REQUEST(${bondingDevice?.address})")
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState:Int
                        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    val previousBondState:Int
                        = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                    val device: BluetoothDevice?
                        = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    // BluetoothDevice.BOND_NONE    10
                    // BluetoothDevice.BOND_BONDING 11
                    // BluetoothDevice.BOND_BONDED  12
                    // Log.d(TAG, "ACTION_BOND_STATE_CHANGED(${device?.address}): $previousBondState => $bondState")
                    bondingDevice?.let {
                        if( bondingDevice?.address == device?.address
                            && previousBondState == BluetoothDevice.BOND_BONDING
                            && bondState == BluetoothDevice.BOND_BONDED) {
                            GlobalScope.launch {
                                // Log.d(TAG, "Устройство было сопряжено. PreviousBondState: $previousBondState, BondState: $bondState, device: ${device?.address}")
                                // _paired.tryEmit(device)
                                receiverCallbacks.forEach { callback ->
                                    device?.let { btDevice ->
                                        callback.onBluetoothPaired(btDevice.toBtLeDevice())
                                    }
                                }
                                bondingDevice = null
                            }
                        }
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                }
                else -> {

                }
            }
        }
    }

    fun addEventListener(receiverCallback: ReceiverCallback) {
        receiverCallbacks.add(receiverCallback)
    }
}