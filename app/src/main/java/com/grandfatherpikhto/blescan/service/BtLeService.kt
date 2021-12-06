package com.grandfatherpikhto.blescan.service

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeService: Service() {
    /** */
    companion object {
        const val TAG:String = "BtLeService"
    }

    /** */
    enum class State(val value:Int) {
        Unknown(0x00),
        Disconnected(0x01),
        Connecting(0x02),
        Connected(0x03),
        Rescan(0x04),
        Error(0xFE),
        FatalError(0xFF)
    }

    data class CharWrite(val gatt: BluetoothGatt?, val characteristic: BluetoothGattCharacteristic?, val status:Int)

    /** */
    private val sharedState = MutableStateFlow<State>(State.Error)
    val state  = sharedState.asStateFlow()

    /** */
    private val bluetoothManager:BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    /** */
    private val bluetoothAdapter:BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }
    /** */
    private var bluetoothAddress:String ?= null
    /** */
    private var bluetoothDevice:BluetoothDevice ?= null
    /** */
    private var bluetoothGatt:BluetoothGatt ?= null
    /** */
    private val charWriteMutex = Mutex()
    /** */
    private val _gatt = MutableStateFlow<BluetoothGatt?>(null)
    val gatt = _gatt.asStateFlow()

    /** Binder given to clients */
    private val binder = LocalBinder()
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): BtLeService = this@BtLeService
    }

    override fun onBind(p0: Intent?): IBinder? {
        // Log.d(TAG, "Сервис связан")
        sharedState.tryEmit(State.Disconnected)
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // Log.d(TAG, "Сервис отвязан")
        close()
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        super.onCreate()

        if(charWriteMutex.isLocked) charWriteMutex.unlock()
        GlobalScope.launch {
            BtGattCallback.state.collect {  gattState ->
                Log.d(TAG, "State: $gattState")
                when(gattState) {
                    BtGattCallback.State.Disconnected -> {
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                        Log.e(TAG, "Disconnected")
                        doRescan()
                        sharedState.tryEmit(State.Disconnected)
                    }
                    BtGattCallback.State.Connecting -> {
                        sharedState.tryEmit(State.Connecting)
                    }
                    BtGattCallback.State.Error -> {
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                        sharedState.tryEmit(State.Error)
                        doRescan()
                    }
                    BtGattCallback.State.Discovered -> {
                        sharedState.tryEmit(State.Connected)
                    }
                    BtGattCallback.State.FatalError -> {
                        sharedState.tryEmit(State.FatalError)
                    }
                    else -> {

                    }
                }
            }
        }

        GlobalScope.launch {
            BtLeScanServiceConnector.device.collect { device ->
                device?.let {
                    if(sharedState.value == State.Rescan) {
                        connect(device.address)
                    }
                }
            }
        }

        GlobalScope.launch {
            BCReceiver.paired.collect { pairedDevice ->
                pairedDevice?.let {
                    Log.d(TAG, "Paired device: ${pairedDevice?.address}")
                    doConnect()
                }
            }
        }
    }

    override fun onDestroy() {
        bluetoothGatt?.close()
        super.onDestroy()
    }

    private fun doRescan() {
        if(bluetoothAddress != null) {
            BtLeScanServiceConnector.scanLeDevice(
                addresses = listOf(bluetoothAddress!!), mode = BtLeScanService.Mode.StopOnFind
            )
            sharedState.tryEmit(State.Rescan)
        }
    }

    /**
     *
     */
    private fun doConnect() {
        Log.d(TAG, "Пытаемся подключиться к $bluetoothAddress")
        bluetoothGatt = bluetoothDevice?.connectGatt(
            applicationContext,
            bluetoothDevice!!.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
            BtGattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
        sharedState.tryEmit(State.Connecting)
    }

    /**
     *
     */
    fun close() {
        bluetoothGatt?.close()
    }

    /**
     * Если устройство не сопряжено, сопрягаем его и ждём оповещение сопряжения
     * после получения, повторяем попытку подключения.
     */
    fun connect(address: String? = null) {
        if(address != null) {
            bluetoothAddress = address
            bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            if(bluetoothDevice != null) {
                if(bluetoothDevice!!.bondState == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "Пытаемся сопрячь устройство $address")
                    bluetoothDevice!!.createBond()
                } else {
                    doConnect()
                }
            } else {
                // TODO: Это неправильно. Надо генерировать состояние, а не вызывать doRescan()!!!
                doRescan()
            }
        }
    }
}