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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@DelicateCoroutinesApi
@RequiresApi(Build.VERSION_CODES.M)
object BcReceiver : BroadcastReceiver() {
    const val TAG:String = "BCReceiver"

    private var bluetoothManager:BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    private var bondingDevice:BluetoothDevice? = null

    private val _paired = MutableStateFlow<BluetoothDevice?>(null)
    val paired = _paired.asSharedFlow()

    private val _btState = MutableStateFlow<Int>(-1)
    val btState = _btState.asStateFlow()


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
        // Log.d(TAG, "broadcastReceiver: ${intent?.action}")
        if (bluetoothManager == null) {
            bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        }
        if (bluetoothAdapter == null) {
            bluetoothAdapter = bluetoothManager?.adapter
        }
        
        if(intent != null) {
            when(intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state    = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    _btState.tryEmit(state)
                    Log.d(TAG, "ACTION_STATE_CHANGED($state) ${state == BluetoothAdapter.STATE_ON}")
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
                                Log.d(TAG, "Устройство было сопряжено. PreviousBondState: $previousBondState, BondState: $bondState, device: ${device?.address}")
                                _paired.tryEmit(device)
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
}