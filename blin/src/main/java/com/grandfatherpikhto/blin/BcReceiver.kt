package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BcReceiver(private val btLeInterface: BtLeInterface) : BroadcastReceiver() {
    companion object {
        const val TAG: String = "BCReceiver"
    }

    /** */
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()

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
        if (intent != null) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    val old =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR);
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    // Log.d(TAG, "ACTION_STATE_CHANGED($state) ${state == BluetoothAdapter.STATE_ON}")
                    bluetoothInterface.changeBluetoothState(device, old, state)
                }
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    bluetoothInterface.bluetoothPairing = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    Log.d(TAG, "ACTION_PAIRING_REQUEST(${bluetoothInterface.bluetoothPairing?.address})")
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState: Int = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    val previousBondState: Int =
                        intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    // BluetoothDevice.BOND_NONE    10
                    // BluetoothDevice.BOND_BONDING 11
                    // BluetoothDevice.BOND_BONDED  12
                    // Log.d(TAG, "ACTION_BOND_STATE_CHANGED(${device?.address}): $previousBondState => $bondState")
                    bluetoothInterface.changeBluetoothBondState(device, previousBondState, bondState)
                }
                BluetoothDevice.ACTION_FOUND -> {
                }
                else -> {

                }
            }
        }
    }

    fun destroy() {

    }
}