package com.grandfatherpikhto.blescan.service

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
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
    private val sharedGatt = MutableStateFlow<BluetoothGatt?>(null)
    val gatt = sharedGatt.asStateFlow()

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
    private val btGattCallback:BtGattCallback by lazy {
        BtGattCallback()
    }

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

    /**
     *
     */
    override fun onBind(p0: Intent?): IBinder? {
        // Log.d(TAG, "Сервис связан")
        sharedState.tryEmit(State.Disconnected)
        return binder
    }

    /**
     *
     */
    override fun onUnbind(intent: Intent?): Boolean {
        // Log.d(TAG, "Сервис отвязан")
        bluetoothGatt?.disconnect()
        return super.onUnbind(intent)
    }

    /**
     *
     */
    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        super.onCreate()

        if(charWriteMutex.isLocked) charWriteMutex.unlock()

        GlobalScope.launch {
            btGattCallback.state.collect {  gattState ->
                Log.d(TAG, "State: $gattState")
                when(gattState) {
                    BtGattCallback.State.Disconnected -> {
                        doClose()
                        doRescan()
                    }
                    BtGattCallback.State.Connecting -> {
                        sharedState.tryEmit(State.Connecting)
                    }
                    BtGattCallback.State.Error -> {
                        sharedState.tryEmit(State.Error)
                        doClose()
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
            BcReceiver.paired.collect { pairedDevice ->
                pairedDevice?.let {
                    Log.d(TAG, "Paired device: ${pairedDevice?.address}")
                    doConnect()
                }
            }
        }

        GlobalScope.launch {
            btGattCallback.gatt.collect { value ->
                sharedGatt.tryEmit(value)
            }
        }
    }

    /**
     *
     */
    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        bluetoothGatt?.close()
        super.onDestroy()
    }

    /**
     * Запрос на пересканирование с адресом устройства и остановкой сканирования
     * после обнаружения устройства
     */
    private fun doRescan() {
        if(bluetoothAddress != null) {
            BtLeScanServiceConnector.scanLeDevices(
                addresses = listOf(bluetoothAddress!!), mode = BtLeScanService.Mode.StopOnFind
            )
            sharedState.tryEmit(State.Rescan)
        }
    }

    /**
     * Пытается подключиться к сервису GATT
     * После подключения начинает работать синглетон BtGattCallback
     */
    private fun doConnect() {
        Log.d(TAG, "Пытаемся подключиться к $bluetoothAddress")
        bluetoothGatt = bluetoothDevice?.connectGatt(
            applicationContext,
            bluetoothDevice!!.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
            btGattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
        sharedState.tryEmit(State.Connecting)
    }

    /**
     * Закрывает и обнуляет GATT.
     * Генерирует событие об отключении
     */
    private fun doClose() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        sharedState.tryEmit(State.Disconnected)
    }

    /**
     *
     */
    fun close() {
        bluetoothGatt?.disconnect()
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
                doRescan()
            }
        }
    }
}