package com.grandfatherpikhto.blescan.service

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Обратные вызовы работы с GATT
 */
@DelicateCoroutinesApi
@InternalCoroutinesApi
object BtGattCallback  : BluetoothGattCallback() {
    const val TAG:String = "BtGattCallback"
    const val MAX_TRY_CONNECT = 6

    enum class State(val value:Int) {
        Unknown(0x0),
        Disconnecting(0x01),
        Disconnected(0x02),
        Connecting(0x03),
        Connected(0x04),
        Discovering(0x05),
        Discovered(0x06),
        CharWrited(0x07),
        CharReaded(0x08),
        Error(0xFE),
        FatalError(0xFF),
    }


    private val _state = MutableStateFlow(State.Unknown)
    val state = _state.asStateFlow()

    private val _writed = MutableStateFlow<BtLeService.CharWrite?>(null)
    val writed = _writed.asStateFlow()

    private val _gatt = MutableStateFlow<BluetoothGatt?>(null)
    val gatt = _gatt.asStateFlow()

    private var tryConnectCounter = 0

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

        super.onConnectionStateChange(gatt, status, newState)
        when (newState) {
            BluetoothProfile.STATE_DISCONNECTED -> {
                _state.tryEmit(State.Disconnected)
                Log.e(TAG, "Подключение закрыто")
            }
            BluetoothProfile.STATE_CONNECTING -> {
                _state.tryEmit(State.Connecting)
            }
            BluetoothProfile.STATE_CONNECTED -> {
                _state.tryEmit(State.Connected)
                GlobalScope.launch {
                    if(gatt!!.discoverServices()) {
                        _state.tryEmit(State.Discovering)
                        Log.d(TAG, "Начали исследовать сервисы")
                    } else {
                        Log.e(TAG, "Ошибка. Не можем исследовать сервисы")
                        _state.tryEmit(State.Error)
                    }
                }
                tryConnectCounter = 0
            }
            BluetoothProfile.STATE_DISCONNECTING -> {
                _state.tryEmit(State.Disconnecting)
            }
            else -> {
            }
        }
        if(status == 6 || status == 133) {
            Log.d(TAG, "onConnectionStateChange $status $newState запустить рескан")
            if (tryConnectCounter >= MAX_TRY_CONNECT - 1) {
                tryConnectCounter = 0
                _state.tryEmit(State.FatalError)
            } else {
                _state.tryEmit(State.Error)
                tryConnectCounter++
            }
        }
    }

    override fun onServicesDiscovered(btgatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(btgatt, status)
        if(status == BluetoothGatt.GATT_SUCCESS) {
            if(btgatt != null) {
                _gatt.tryEmit(btgatt)
                _state.tryEmit(State.Discovered)
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        _writed.tryEmit(BtLeService.CharWrite(gatt, characteristic, status))
    }
}