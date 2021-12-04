package com.grandfatherpikhto.blescan.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@DelicateCoroutinesApi
@RequiresApi(Build.VERSION_CODES.M)
object BCReceiver : BroadcastReceiver() {
    const val TAG:String = "BCReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        // Log.d(TAG, "broadcastReceiver: ${intent?.action}")
        if(intent != null) {
            when(intent.action) {
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    GlobalScope.launch {
                        // sharedPairing.tryEmit(Pairing.PairingRequest)
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState:Int
                        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    val previousBondState:Int
                        = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                    // BluetoothDevice.BOND_NONE    10
                    // BluetoothDevice.BOND_BONDING 11
                    // BluetoothDevice.BOND_BONDED  12
                    // Log.d(TAG, "Изменился статус сопряжения ${sharedAddress.value}: $bondState, $previousBondState")
                    if(previousBondState == BluetoothDevice.BOND_NONE
                            && bondState == BluetoothDevice.BOND_BONDED) {
                        GlobalScope.launch {
                            // TODO: Разобраться, почему всё-время идёт уведомление, о том, что BOND_STATE_CHANGED
                            Log.d(TAG, "Устройство было сопряжено. Запустить рескан $bondState $previousBondState")
                            // sharedPairing.tryEmit(Pairing.Paired)
                            // doRescanDevice()
                        }
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    // Log.d(TAG, "Устройство найдено")
                    // connect()
                }
                else -> {

                }
            }
        }
    }
}