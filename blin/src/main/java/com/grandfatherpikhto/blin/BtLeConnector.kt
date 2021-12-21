package com.grandfatherpikhto.blin

import android.bluetooth.*
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.net.InterfaceAddress

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeConnector(private val btLeInterface: BtLeInterface) {
    /** */
    companion object {
        const val TAG:String = "BtLeService"
    }
    /**
     * Список состояний GATT, процедуры подключения, пересканирования
     */
    enum class State(val value:Int) {
        Unknown(0x00),        // Просто, для инициализации
        Disconnecting(0x03),  // Отключение от GATT
        Disconnected(0x02),   // Отключены
        Connecting(0x03),     // Процесс подключения к GATT
        Connected(0x04),      // Подключены
        Discovering(0x05),    // Начали исследовать сервисы
        Discovered(0x06),     // Сервисы исследованы
        Rescan(0x07),         // Запущено пересканирование по адресу устройства
        CharWrited(0x08),     // Характеристика записана
        CharReaded(0x09),     // Характеристика прочитана
        CharChanged(0x0A),    // Дескриптор изменён
        DescrWrited(0x0B),    // Дескриптор записан
        DescrReaded(0x0C),    // Дескриптор прочитан
        ServiceChanged(0x0D), // Сервис изменился
        Error(0xFE),          // Получена ошибка
        FatalError(0xFF)      // Получена фатальная ошибка. Возвращаемся к Фрагменту сканирования устройств
    }

    /** */
    private val charWriteMutex = Mutex()
    /** */
    private var leGattCallback: LeGattCallback? = null
    /** */
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()
    /** Нужно для того, чтобы когда вызывается функция close(), не запускалось бы
     * повторное событие подключения
     **/
    private var reconnect:Boolean = true
    /** */
    val connectorListener = object: BluetoothListener {
        override fun onChangeConnectorState(oldState: State, newState: State) {
            super.onChangeConnectorState(oldState, newState)
            when (newState) {
                State.Disconnected -> {
                    doClose()
                    if(reconnect) {
                        doRescan()
                    }
                }
                State.Error -> {
                    doClose()
                    if(reconnect) {
                        doRescan()
                    }
                }
                else -> {

                }
            }
        }

        override fun onFindDevice(btLeDevice: BluetoothDevice?) {
            super.onFindDevice(btLeDevice)
            if (bluetoothInterface.scannerState == BtLeScanner.State.Stopped
                && bluetoothInterface.connectorState == State.Rescan
            ) {
                if(reconnect) {
                    doConnect()
                }
            }
        }

        /**
         * TODO: Не забудь подключить в BtLeService событие Paired!!!
         */
        override fun onBluetoothPaired(btLeDevice: BluetoothDevice?) {
            super.onBluetoothPaired(btLeDevice)
            doConnect()
        }
    }
    /**
     *
     */
    init {
        Log.d(TAG, "onCreate()")

        if(charWriteMutex.isLocked) charWriteMutex.unlock()
        leGattCallback = LeGattCallback()
        bluetoothInterface.addListener(connectorListener)
    }

    /**
     * Запрос на пересканирование с адресом устройства и остановкой сканирования
     * после обнаружения устройства
     */
    private fun doRescan() {
        if(bluetoothInterface.bluetoothDevice != null) {
            btLeInterface.scanner.scanLeDevices(
                addresses = bluetoothInterface.currentDevice!!.address,
                mode = BtLeScanner.Mode.StopOnFind
            )
            bluetoothInterface.connectorState = State.Rescan
        }
    }

    /**
     * Пытается подключиться к сервису GATT
     * После подключения начинает работать синглетон BtGattCallback
     */
    private fun doConnect() {
        Log.d(TAG, "Пытаемся подключиться к ")
        bluetoothInterface.bluetoothDevice?.let { device ->
            device.connectGatt(
                btLeInterface.context,
                device.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
                leGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
            bluetoothInterface.connectorState = State.Connecting
        }
    }

    /**
     * Закрывает и обнуляет GATT.
     * Генерирует событие об отключении
     */
    private fun doClose() {
        bluetoothInterface.bluetoothGatt?.close()
        bluetoothInterface.bluetoothGatt = null
    }

    /**
     * Дождаться состояния Disconnect.
     * Если этого не сделать, устройство в течение 30-180 секунд
     * будет недоступно для повторного подключения и сканирования
     */
    fun close() {
        reconnect = false
        runBlocking {
            launch {
                bluetoothInterface.bluetoothGatt?.let { gatt ->
                    gatt.disconnect()
                    while (bluetoothInterface.connectorState != State.Disconnected) {
                        delay(100)
                    }
                    bluetoothInterface.bluetoothGatt?.close()
                }
            }
        }
    }

    /**
     *
     */
    fun connect(address: String) {
        bluetoothInterface.bluetoothAdapter?.getRemoteDevice(address).let { device ->
            bluetoothInterface.bluetoothDevice = device
            connect()
        }
    }

    fun connect(bluetoothDevice: BluetoothDevice) {
        bluetoothInterface.bluetoothDevice = bluetoothDevice
        connect()
    }

    /**
     * Если устройство не сопряжено, сопрягаем его и ждём оповещение сопряжения
     * после получения, повторяем попытку подключения.
     */
    private fun connect() {
        reconnect = true
        if (bluetoothInterface.currentDevice != null) {
            if (bluetoothInterface.bluetoothDevice!!.bondState
                == BluetoothDevice.BOND_NONE) {
                Log.d(TAG, "Пытаемся сопрячь устройство ${bluetoothInterface.currentDevice?.address}")
                bluetoothInterface.bluetoothDevice!!.createBond()
            } else {
                doConnect()
            }
        } else {
            doRescan()
        }
    }

    fun destroy() {
        bluetoothInterface.removeListener(connectorListener)
    }
}