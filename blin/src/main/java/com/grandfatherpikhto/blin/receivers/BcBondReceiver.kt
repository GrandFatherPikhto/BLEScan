package com.grandfatherpikhto.blin.receivers

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.grandfatherpikhto.blin.BleBondManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class BcBondReceiver constructor(private val bleBondManager: BleBondManager,
                                 private val dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : BroadcastReceiver() {

    private val logTag = this.javaClass.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {
        if ( context != null && intent != null ) {
            when (intent.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState: Int = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    val previousBondState: Int =
                        intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                    val bluetoothDevice: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    // Log.d(TAG, "ACTION_BOND_STATE_CHANGED(${device?.address}): $previousBondState => $bondState")
                    bleBondManager.onSetBondingDevice(bluetoothDevice, previousBondState, bondState)
                }
                else -> {

                }
            }
        }
    }
}