package com.grandfatherpikhto.blin.orig

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.grandfatherpikhto.blin.BleGattCallback
import com.grandfatherpikhto.blin.buffer.BleCharacteristicNotify
import com.grandfatherpikhto.blin.data.BleGattItem
import com.grandfatherpikhto.blin.idling.ScanIdling
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

abstract class AbstractBleGattManager constructor(private val context: Context,
                                                  private val bleScanManager: AbstractBleScanManager,
                                                  dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    companion object {
        const val MAX_ATTEMPTS = 6
        val NOTIFY_DESCRIPTOR_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb".uppercase())
        const val WAIT_TIMEOUT = 2000L
    }

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter
            = bluetoothManager.adapter
    private val applicationContext:Context get() = context.applicationContext

    enum class State(val value:Int) {
        Disconnected  (0x00), // Отключены
        Disconnecting (0x01), // Отключаемся
        Connecting    (0x02), // Подключаемся
        Connected     (0x02), // Подключены
        Error         (0xFF), // Получена ошибка
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

    private val mutableStateFlowBluetoothGatt = MutableStateFlow<BluetoothGatt?>(null)
    val stateFlowBluetoothGatt get() = mutableStateFlowBluetoothGatt.asStateFlow()
    val bluetoothGatt:BluetoothGatt? get() = mutableStateFlowBluetoothGatt.value

    private val mutableListNotifiedCharacteristic = mutableListOf<BluetoothGattCharacteristic>()
    val notifiedCharacteristic get() = mutableListNotifiedCharacteristic.toList()

    private val mutableSharedFlowCharacteristicNotify = MutableSharedFlow<BleCharacteristicNotify>(replay = 100)
    val sharedFlowCharacteristicNotify get() = mutableSharedFlowCharacteristicNotify.asSharedFlow()

    private var connectIdling: ScanIdling? = null

    init {
        scope.launch {
            bleScanManager.stateFlowScanState.collect { scanState ->
                if (attemptReconnect && bluetoothDevice != null &&
                    scanState == AbstractBleScanManager.State.Stopped &&
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

    fun onDestroy() {
        disconnect()
    }

    /**
     *
     */
    private fun doRescan() {
        if (attemptReconnect && reconnectAttempts < MAX_ATTEMPTS) {
            bluetoothDevice?.let { device ->
                bleScanManager.startScan(addresses = listOf(device.address),
                    stopTimeout = 2000L,
                    stopOnFind = true)
            }
        }
    }

    fun connect(address:String) : BluetoothGatt? {
        val validAddress = address.uppercase()
        /**
         * https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#enable()
         */
        if (!bluetoothAdapter.isEnabled) {
            return null
        }

        Log.d(tagLog, "connect($validAddress)")
        if (connectState == State.Disconnected) {
            connectIdling?.idling = false
            if (BluetoothAdapter.checkBluetoothAddress(validAddress)) {
                bluetoothAdapter.getRemoteDevice(validAddress)?.let { device ->
                    mutableStateFlowConnectState.tryEmit(State.Connecting)
                    bluetoothDevice = device
                    attemptReconnect = true
                    reconnectAttempts = 0
                    doConnect()
                }
            }
        }

        return null
    }

    @SuppressLint("MissingPermission")
    private fun doConnect() : BluetoothGatt? {
        bluetoothDevice?.let { device ->
            reconnectAttempts ++

            return device.connectGatt(
                applicationContext,
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
            mutableListNotifiedCharacteristic.forEach { characteristic ->
                disableNotifyCharacteristic(characteristic)
            }

            withTimeout(WAIT_TIMEOUT) {
                while (mutableListNotifiedCharacteristic.isNotEmpty()) {
                    delay(20)
                }
            }

            Log.d(tagLog, "doDisconnect($bluetoothDevice)")
            gatt.disconnect()
            withTimeout(WAIT_TIMEOUT) {
                while (connectState != State.Disconnected) {
                    delay(20)
                }
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
                mutableStateFlowBluetoothGatt.tryEmit(it)
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
                    mutableStateFlowBluetoothGatt.tryEmit(null)
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

    fun addGattData(bleGattData: BleGattItem) = bleGattCallback.addGattData(bleGattData)

    fun isCharacteristicNotified(bluetoothGattCharacteristic: BluetoothGattCharacteristic) : Boolean =
        mutableListNotifiedCharacteristic.isNotEmpty()
                && mutableListNotifiedCharacteristic.contains(bluetoothGattCharacteristic)

    @SuppressLint("MissingPermission")
    private fun disableNotifyCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            bluetoothGattCharacteristic.getDescriptor(NOTIFY_DESCRIPTOR_UUID)
                ?.let { bluetoothGattDescriptor ->
                    bluetoothGattDescriptor.value =
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    addGattData(BleGattItem(bluetoothGattDescriptor, BleGattItem.Type.Write))
                    gatt.setCharacteristicNotification(bluetoothGattCharacteristic, false)
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifyCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            bluetoothGattCharacteristic.getDescriptor(NOTIFY_DESCRIPTOR_UUID)
                ?.let { bluetoothGattDescriptor ->
                    bluetoothGattDescriptor.value =
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    addGattData(BleGattItem(bluetoothGattDescriptor, BleGattItem.Type.Write))
                    gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true)
                }
        }
    }

    @SuppressLint("MissingPermission")
    fun notifyCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        if (isCharacteristicNotified(bluetoothGattCharacteristic)) {
            disableNotifyCharacteristic(bluetoothGattCharacteristic)
        } else {
            enableNotifyCharacteristic(bluetoothGattCharacteristic)
        }
    }

    fun notifyCharacteristic(bleGattData: BleGattItem) {
        bluetoothGatt?.let { gatt ->
            gatt.getService(bleGattData.uuidService)?.let { service ->
                service.getCharacteristic(bleGattData.uuidCharacteristic).let { char ->
                    notifyCharacteristic(char)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic) : Boolean {
        bluetoothGatt?.let { gatt ->
            addGattData(BleGattItem(bluetoothGattCharacteristic, BleGattItem.Type.Read))
        }

        return false
    }

    @SuppressLint("MissingPermission")
    fun readDescriptor(bluetoothGattDescriptor: BluetoothGattDescriptor) : Boolean {
        bluetoothGatt?.let { gatt ->
            addGattData(BleGattItem(bluetoothGattDescriptor, BleGattItem.Type.Read))
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
        if (gatt != null && characteristic != null) {
            Log.d(tagLog, "onCharacteristicChanged(${characteristic.uuid})")
        }
    }
}