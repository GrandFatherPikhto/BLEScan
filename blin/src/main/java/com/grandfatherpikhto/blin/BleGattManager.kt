package com.grandfatherpikhto.blin

import android.annotation.SuppressLint
import android.bluetooth.*
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.grandfatherpikhto.blin.buffer.BleCharacteristicNotify
import com.grandfatherpikhto.blin.buffer.GattData
import com.grandfatherpikhto.blin.data.BleGatt
import com.grandfatherpikhto.blin.idling.ScanIdling
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class BleGattManager constructor(private val bleManager: BleManager,
                                 dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : DefaultLifecycleObserver {

    companion object {
        const val MAX_ATTEMPTS = 6
        val NOTIFY_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb".uppercase())
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

    private val tagLog = this.javaClass.simpleName
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

    private val mutableListNotifiedCharacteristic = mutableListOf<BluetoothGattCharacteristic>()
    val notifiedCharacteristic get() = mutableListNotifiedCharacteristic.toList()

    private val mutableSharedFlowCharacteristicNotify = MutableSharedFlow<BleCharacteristicNotify>(replay = 100)
    val sharedFlowCharacteristicNotify get() = mutableSharedFlowCharacteristicNotify.asSharedFlow()

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
        Log.d(tagLog, "connect($address)")
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
        Log.d(tagLog, "disconnect()")
        attemptReconnect = false
        reconnectAttempts = 0
        mutableStateFlowConnectState.tryEmit(State.Disconnecting)
        doDisconnect()
    }

    @SuppressLint("MissingPermission")
    private fun doDisconnect() = runBlocking {
        bluetoothGatt?.let { gatt ->
            Log.d(tagLog, "doDisconnect($bluetoothDevice)")
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
        Log.d(tagLog, "onGattDiscovered(status = )")
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

    fun onCharacteristicRead(gatt: BluetoothGatt?,
                             characteristic: BluetoothGattCharacteristic?,
                             status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS
            && gatt != null
            && characteristic != null) {
            // val value = characteristic.value.joinToString (", "){ String.format("%02X", it) }
            // Log.d(tagLog, "onCharacteristicRead(${characteristic.uuid}, $value)")
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

    fun writeGattData(gattData: GattData) = bleGattCallback.writeGattData(gattData)

    fun isCharacteristicNotified(bluetoothGattCharacteristic: BluetoothGattCharacteristic) : Boolean =
        mutableListNotifiedCharacteristic.isNotEmpty()
                && mutableListNotifiedCharacteristic.contains(bluetoothGattCharacteristic)

    @SuppressLint("MissingPermission")
    fun notifyCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            bluetoothGattCharacteristic.getDescriptor(NOTIFY_DESCRIPTOR_UUID)
                ?.let { bluetoothGattDescriptor ->
                    if (isCharacteristicNotified(bluetoothGattCharacteristic)) {
                        gatt.setCharacteristicNotification(bluetoothGattCharacteristic, false)
                        // Log.d(tagLog, "notifyCharacteristic(${bluetoothGattCharacteristic.uuid}, disable)")
                        bluetoothGattDescriptor.value =
                            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        writeGattData(GattData(bluetoothGattDescriptor))
                    } else {
                        gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)
                        // Log.d(tagLog, "notifyCharacteristic(${bluetoothGattCharacteristic.uuid}, enable)")
                        bluetoothGattDescriptor.value =
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        writeGattData(GattData(bluetoothGattDescriptor))
                    }
                }
        }
    }

    fun notifyCharacteristic(gattData: GattData) {
        bluetoothGatt?.let { gatt ->
            gatt.getService(gattData.uuidService)?.let { service ->
                service.getCharacteristic(gattData.uuidCharacteristic).let { char ->
                    notifyCharacteristic(char)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) : Boolean {
        bluetoothGatt?.let { gatt ->
            return gatt.readCharacteristic(bluetoothGattCharacteristic)
        }

        return false
    }

    @SuppressLint("MissingPermission")
    fun readGattData(gattData: GattData) : Boolean {
        bluetoothGatt?.let { gatt ->
            gatt.getService(gattData.uuidService)?.let { service ->
                service.getCharacteristic(gattData.uuidCharacteristic)?.let { char ->
                    if (gattData.uuidDescriptor == null) return readCharacteristic(char)
                    else char.getDescriptor(gattData.uuidDescriptor)?.let { descr ->
                            return readDescriptor(descr)
                    }
                }
            }
        }

        return false
    }



    @SuppressLint("MissingPermission")
    fun readDescriptor(bluetoothGattDescriptor: BluetoothGattDescriptor) : Boolean {
        bluetoothGatt?.let { gatt ->
            return gatt.readDescriptor(bluetoothGattDescriptor)
        }

        return false
    }

    @SuppressLint("MissingPermission")
    fun onCharacteristicWrite(gatt: BluetoothGatt?,
                              characteristic: BluetoothGattCharacteristic?,
                              status: Int) {
        if (gatt != null && characteristic != null && status == BluetoothGatt.GATT_SUCCESS) {
            val value = characteristic.value.joinToString (", "){ String.format("%02X", it) }
            Log.d(tagLog, "onCharacteristicWrite(${characteristic.uuid}, $value)")
            mutableSharedFlowCharacteristic.tryEmit(characteristic)
        }
    }

    private fun descriptorNotify(bluetoothGattDescriptor: BluetoothGattDescriptor) {
        if (bluetoothGattDescriptor.uuid == NOTIFY_DESCRIPTOR_UUID) {
            val notify = ByteBuffer
                .wrap(bluetoothGattDescriptor.value)
                .order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            when(notify) {
                0 -> {
                    if (isCharacteristicNotified(bluetoothGattDescriptor.characteristic)) {
                        Log.d(tagLog, "onCharacteristicChanged(${bluetoothGattDescriptor.characteristic.uuid}, notifyDisable)")
                        mutableListNotifiedCharacteristic.remove(bluetoothGattDescriptor.characteristic)
                        mutableSharedFlowCharacteristicNotify
                            .tryEmit(BleCharacteristicNotify(bluetoothGattDescriptor.characteristic.uuid, false))
                    }
                }
                1 -> {
                    if (!isCharacteristicNotified(bluetoothGattDescriptor.characteristic)) {
                        Log.d(tagLog, "onCharacteristicChanged(${bluetoothGattDescriptor.characteristic.uuid}, notifyEnable)")
                        mutableListNotifiedCharacteristic.add(bluetoothGattDescriptor.characteristic)
                        mutableSharedFlowCharacteristicNotify
                            .tryEmit(BleCharacteristicNotify(bluetoothGattDescriptor.characteristic.uuid, true))
                    }
                }
                2 -> {

                }
                else -> {

                }
            }
        }

    }

    @SuppressLint("MissingPermission")
    fun onDescriptorWrite(gatt: BluetoothGatt?,
                          descriptor: BluetoothGattDescriptor?,
                          status: Int) {
        if (gatt != null && descriptor != null && status == BluetoothGatt.GATT_SUCCESS) {
            mutableSharedFlowDescriptor.tryEmit(descriptor)
            descriptorNotify(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    fun onCharacteristicChanged(gatt: BluetoothGatt?,
                          characteristic: BluetoothGattCharacteristic?) {
    }
}