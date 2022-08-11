package com.grandfatherpikhto.blin

import android.annotation.SuppressLint
import android.bluetooth.*
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.grandfatherpikhto.blin.buffer.GattData
import com.grandfatherpikhto.blin.buffer.OutputBuffer
import com.grandfatherpikhto.blin.data.BleGatt
import com.grandfatherpikhto.blin.idling.ScanIdling
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class BleGattManager constructor(private val bleManager: BleManager,
                                 dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : DefaultLifecycleObserver {

    companion object {
        const val MAX_ATTEMPTS = 6
    }
    
    enum class State(val value:Int) {
        Disconnected  (0x00), // Отключены
        Disconnecting (0x01), // Отключаемся
        Connecting    (0x02), // Подключаемся
        Connected     (0x02), // Подключены
        Error         (0xFF), // Получена ошибка
    }

    enum class GattStatus (val value: Int) {
        GattSuccess (BluetoothGatt.GATT_SUCCESS),
        GattFailure(BluetoothGatt.GATT_FAILURE)
    }

    private val logTag = this.javaClass.simpleName
    private val bleGattCallback  = BleGattCallback(this, dispatcher)
    private var bluetoothDevice: BluetoothDevice? = null
    val device get() = bluetoothDevice
    private var scope = CoroutineScope(dispatcher)

    private var attemptReconnect = true
    private var reconnectAttempts = 0
    val attempt get() = reconnectAttempts

    private val mutableSharedFlowCharacteristic
        = MutableSharedFlow<BluetoothGattCharacteristic>(replay = 100)
    val sharedFlowCharacteristic get() = mutableSharedFlowCharacteristic.asSharedFlow()

    private val mutableSharedFlowDescriptor
        = MutableSharedFlow<BluetoothGattDescriptor>(replay = 100)
    val sharedFlowDescriptor get() = mutableSharedFlowDescriptor.asSharedFlow()

    private val mutableStateFlowConnectState  = MutableStateFlow(State.Disconnected)
    val stateFlowConnectState get() = mutableStateFlowConnectState.asStateFlow()
    val connectState get() = mutableStateFlowConnectState.value

    private val mutableSharedFlowConnectStateCode = MutableSharedFlow<Int>(replay = 100)
    val stateFlowConnectStateCode get() = mutableSharedFlowConnectStateCode.asSharedFlow()

    private val mutableStateFlowGatt = MutableStateFlow<BluetoothGatt?>(null)
    val stateFlowGatt get() = mutableStateFlowGatt.asStateFlow()
    val bluetoothGatt:BluetoothGatt? get() = mutableStateFlowGatt.value

    private val mutableStateFlowBleGatt = MutableStateFlow<BleGatt?>(null)
    val stateFlowBleGatt get() = mutableStateFlowBleGatt.asStateFlow()
    val bleGatt get() = mutableStateFlowBleGatt.value

    val bleScanManager = bleManager.bleScanManager

    private var connectIdling: ScanIdling? = null

    init {
        scope.launch {
            bleScanManager.stateFlowScanState.collect { scanState ->
                if (attemptReconnect && bluetoothDevice != null &&
                    scanState == BleScanManager.State.Stopped &&
                    bleScanManager.scanResults.isNotEmpty() &&
                    bleScanManager.scanResults.last().device.address
                        == bluetoothDevice!!.address) {
                    if (reconnectAttempts < MAX_ATTEMPTS) {
                        doConnect()
                    } else {
                        mutableStateFlowConnectState.tryEmit(State.Error)
                        attemptReconnect = false
                    }
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        disconnect()
        super.onDestroy(owner)
    }

    /**
     *
     */
    private fun doRescan() {
        if (attemptReconnect && reconnectAttempts < MAX_ATTEMPTS) {
            bluetoothDevice?.let { device ->
                bleManager.startScan(addresses = listOf(device.address),
                    stopTimeout = 2000L,
                    stopOnFind = true)
            }
        }
    }

    fun connect(address:String) : BluetoothGatt? {
        Log.d(logTag, "connect($address)")
        if (connectState == State.Disconnected) {
            connectIdling?.idling = false
            bleManager.bluetoothAdapter.getRemoteDevice(address)?.let { device ->
                mutableStateFlowConnectState.tryEmit(State.Connecting)
                bluetoothDevice = device
                attemptReconnect = true
                reconnectAttempts = 0
                doConnect()
            }
        }

        return null
    }

    @SuppressLint("MissingPermission")
    private fun doConnect() : BluetoothGatt? {
        bluetoothDevice?.let { device ->
            reconnectAttempts ++

            return device.connectGatt(
                bleManager.applicationContext,
                device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
                bleGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }

        return null
    }

    /**
     * Даёт команду на отключение (gatt.disconnect()).
     * Если статус Disconnected, сразу закрывает подключение gatt.close()
     * Если нет, блокирует поток и ждёт, пока не будет получено состояние
     * Disconnected и после этого закрыает подключение
     * Это нужно для того, чтобы сбросить счётчик подключений
     * Если он переполнится, нужно будет очищать кэш Bluetooth
     */
    fun disconnect() {
        Log.d(logTag, "disconnect()")
        attemptReconnect = false
        reconnectAttempts = 0
        mutableStateFlowConnectState.tryEmit(State.Disconnecting)
        doDisconnect()
    }

    @SuppressLint("MissingPermission")
    private fun doDisconnect() = runBlocking {
        bluetoothGatt?.let { gatt ->
            Log.d(logTag, "doDisconnect($bluetoothDevice)")
            gatt.disconnect()
            while (connectState != State.Disconnected) {
                delay(100)
            }
            gatt.close()
        }
    }

    /**
     * Интерфейсы исследованы. Сбрасываем счётчик переподключений и
     * генерируем событие о полном подключении. Можно принимать и передавать данные
     */
    fun onGattDiscovered(discoveredGatt: BluetoothGatt?, status: Int) {
        Log.d(logTag, "onGattDiscovered(status = )")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            discoveredGatt?.let {
                mutableStateFlowGatt.tryEmit(it)
                mutableStateFlowBleGatt.tryEmit(BleGatt(it))
                mutableStateFlowConnectState.tryEmit(State.Connected)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (newState) {
                BluetoothProfile.STATE_DISCONNECTED -> {
                    mutableStateFlowConnectState.tryEmit(State.Disconnected)
                    mutableStateFlowGatt.tryEmit(null)
                    mutableStateFlowBleGatt.tryEmit(null)
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt?.let {
                        if(!it.discoverServices()) {
                            mutableStateFlowConnectState.tryEmit(State.Error)
                            attemptReconnect  = false
                            reconnectAttempts = 0
                            doDisconnect()
                        }
                    }
                }
                else -> {
                    // Log.e(logTag, "Unknown newState: $newState")
                }
            }
        } else {
            mutableSharedFlowConnectStateCode.tryEmit(newState)
            if (attemptReconnect) {
                if (reconnectAttempts < MAX_ATTEMPTS) {
                    doRescan()
                } else {
                    mutableStateFlowConnectState.tryEmit(State.Error)
                    attemptReconnect  = false
                    reconnectAttempts = 0
                }
            }
        }
    }

    fun writeGattData(gattData: GattData) = bleGattCallback.writeGattData(gattData)

    fun onCharacteristicRead(gatt: BluetoothGatt?,
                             characteristic: BluetoothGattCharacteristic?,
                             status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS
            && gatt != null
            && characteristic != null) {
            mutableSharedFlowCharacteristic.tryEmit(characteristic)
        }
    }

    fun onDescriptorRead(gatt: BluetoothGatt?,
                         descriptor: BluetoothGattDescriptor?,
                         status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS
            && gatt != null
            && descriptor != null) {
            mutableSharedFlowDescriptor.tryEmit(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            gatt.readCharacteristic(bluetoothGattCharacteristic)
        }
    }

    @SuppressLint("MissingPermission")
    fun readDescriptor(bluetoothGattDescriptor: BluetoothGattDescriptor) {
        bluetoothGatt?.let { gatt ->
            gatt.readDescriptor(bluetoothGattDescriptor)
        }
    }

    @SuppressLint("MissingPermission")
    fun onCharacteristicWrite(gatt: BluetoothGatt?,
                              characteristic: BluetoothGattCharacteristic?,
                              status: Int) {
        if (gatt != null && characteristic != null && status == BluetoothGatt.GATT_SUCCESS) {
            mutableSharedFlowCharacteristic.tryEmit(characteristic)
        }
    }

    @SuppressLint("MissingPermission")
    fun onDescriptorWrite(gatt: BluetoothGatt?,
                          descriptor: BluetoothGattDescriptor?,
                          status: Int) {
        if (gatt != null && descriptor != null && status == BluetoothGatt.GATT_SUCCESS) {
            mutableSharedFlowDescriptor.tryEmit(descriptor)
        }
    }
}