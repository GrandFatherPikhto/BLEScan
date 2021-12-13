package com.grandfatherpikhto.blescan.service

import android.bluetooth.*
import android.util.Log
import com.grandfatherpikhto.blescan.model.BtLeDevice
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeConnector(private val service: BtLeService) {
    /** */
    companion object {
        const val TAG:String = "BtLeService"
    }
    /** */
    enum class State(val value:Int) {
        Unknown(0x00),
        Disconnecting(0x01),
        Disconnected(0x02),
        Connecting(0x03),
        Connected(0x04),
        Discovering(0x05),
        Discovered(0x06),
        Rescan(0x07),
        CharWrited(0x08),
        CharReaded(0x09),
        CharChanged(0x0A),
        DescrWrited(0x0B),
        DescrReaded(0x0C),
        ServiceChanged(0x0D),
        Error(0xFE),
        FatalError(0xFF)
    }

    /** */
    private val charWriteMutex = Mutex()
    /** */
    private var leGattCallback:LeGattCallback? = null
    /** */
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()
    /** */
    val connectorListener = object: BluetoothListener {
        override fun onChangeConnectorState(oldState: State, newState: State) {
            super.onChangeConnectorState(oldState, newState)
            when (newState) {
                State.Disconnected -> {
                    doClose()
                    doRescan()
                }
                State.Error -> {
                    doClose()
                    doRescan()
                }
                else -> {

                }
            }
        }

        override fun onFindDevice(btLeDevice: BtLeDevice?) {
            super.onFindDevice(btLeDevice)
            if (bluetoothInterface.scannerState == BtLeScanner.State.Stopped
                && bluetoothInterface.connectorState == State.Rescan
            ) {
                doConnect()
            }
        }

        /**
         * TODO: Не забудь подключить в BtLeService событие Paired!!!
         */
        override fun onBluetoothPaired(btLeDevice: BtLeDevice?) {
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
            service.scanner.scanLeDevices(
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
                service.applicationContext,
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
     *
     */
    fun close() {
        bluetoothInterface.bluetoothGatt?.disconnect()
    }

    fun connect(btLeDevice: BtLeDevice) {
        bluetoothInterface.currentDevice = btLeDevice
        if (bluetoothInterface.currentDevice != null) {
            bluetoothInterface.bluetoothDevice =
                bluetoothInterface.bluetoothAdapter?.getRemoteDevice(bluetoothInterface.currentDevice!!.address)
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

    /**
     * Если устройство не сопряжено, сопрягаем его и ждём оповещение сопряжения
     * после получения, повторяем попытку подключения.
     */
    fun connect(address: String) {
        connect(BtLeDevice(address = address))
    }

    fun destroy() {
        bluetoothInterface.removeListener(connectorListener)
    }
}