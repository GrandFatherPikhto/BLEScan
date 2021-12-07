package com.grandfatherpikhto.blescan.service

import android.bluetooth.*
import android.util.Log
import com.grandfatherpikhto.blescan.model.BtLeDevice
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.lang.Exception

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

    private var bluetoothAddress:String ?= null
    /** */
    private var bluetoothDevice: BluetoothDevice?= null
    /** */
    private var bluetoothGatt: BluetoothGatt?= null
    /** */
    private val charWriteMutex = Mutex()
    /** */
    private var btGattCallback:BtGattCallback? = null
    /** */
    interface ConnectorCallback {
        fun onChangeGattState(state: State) {}
        fun onGattDiscovered(bluetoothGatt: BluetoothGatt) {}
        fun onCharacteristicWrite(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?, state: Int) {}
        fun onCharacteristicRead(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?, state: Int) {}
        fun onDescriptorWrite(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state: Int) {}
        fun onDescriptorRead(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state: Int) {}
        fun onCharacteristickChanged(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?) {}
        fun onServiceChanged(bluetoothGatt: BluetoothGatt?) {}
    }

    /** */
    private val connectorCallbacks: MutableList<ConnectorCallback> = mutableListOf()
    /** */
    private var state:State = State.Unknown

    /**
     *
     */
    init {
        Log.d(TAG, "onCreate()")

        if(charWriteMutex.isLocked) charWriteMutex.unlock()

        btGattCallback = BtGattCallback()
        btGattCallback!!.addEventListener(object: BtGattCallback.GattCallback {
            override fun onChangeState(gattState: State)  {
                changeState(gattState)
            }

            override fun onGattDiscovered(bluetoothGatt: BluetoothGatt) {
                connectorCallbacks.forEach { callback ->
                    callback.onGattDiscovered(bluetoothGatt)
                }
            }
        })

        service.scanner.addEventListener(object: BtLeScanner.ScannerCallback {
            override fun onFindDevice(btLeDevice: BtLeDevice?) {
                btLeDevice?.let { found ->
                    if (state == State.Rescan) {
                        service.scanner.stopScan()
                    }
                }
            }

            override fun onChangeState(scannerState: BtLeScanner.State) {
                super.onChangeState(scannerState)
                if (scannerState == BtLeScanner.State.Stopped
                    && state == State.Rescan
                ) {
                    doConnect()
                }
            }
        })

        service.receiver.addEventListener(object: BcReceiver.ReceiverCallback {
            override fun onBluetoothPaired(btLeDevice: BtLeDevice) {
                super.onBluetoothPaired(btLeDevice)
                doConnect()
            }
        })

        GlobalScope.launch {
            service.receiver.paired.collect { pairedDevice ->
                pairedDevice?.let {
                    Log.d(TAG, "Paired device: ${pairedDevice?.address}")
                    doConnect()
                }
            }
        }
    }

    /**
     * Запрос на пересканирование с адресом устройства и остановкой сканирования
     * после обнаружения устройства
     */
    private fun doRescan() {
        if(bluetoothAddress != null) {
            BtLeServiceConnector.scanLeDevices(
                addresses = arrayOf(bluetoothAddress!!),
                mode = BtLeScanner.Mode.StopOnFind
            )
            changeState(State.Rescan)
        }
    }

    /**
     * Пытается подключиться к сервису GATT
     * После подключения начинает работать синглетон BtGattCallback
     */
    private fun doConnect() {
        Log.d(TAG, "Пытаемся подключиться к $bluetoothAddress")
        bluetoothGatt = bluetoothDevice?.connectGatt(
            service.applicationContext,
            bluetoothDevice!!.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
            btGattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
        changeState(State.Connecting)
    }

    /**
     * Закрывает и обнуляет GATT.
     * Генерирует событие об отключении
     */
    private fun doClose() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    /**
     *
     */
    fun close() {
        bluetoothGatt?.disconnect()
    }

    fun connect(btLeDevice: BtLeDevice) = connect(btLeDevice.address)

    /**
     * Если устройство не сопряжено, сопрягаем его и ждём оповещение сопряжения
     * после получения, повторяем попытку подключения.
     */
    fun connect(address: String) {
        bluetoothAddress = address
        bluetoothDevice = service.adapter.getRemoteDevice(address)
        if (bluetoothDevice != null) {
            if (bluetoothDevice!!.bondState == BluetoothDevice.BOND_NONE) {
                Log.d(TAG, "Пытаемся сопрячь устройство $address")
                bluetoothDevice!!.createBond()
            } else {
                doConnect()
            }
        } else {
            doRescan()
        }
    }

    fun addEventListener(connector: ConnectorCallback) {
        connectorCallbacks.add(connector)
    }

    private fun changeState(gattState: State) {
        state = gattState
        when(gattState) {
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
        connectorCallbacks.forEach { callback ->
            try {
                callback.onChangeGattState(gattState)
            } catch (e : Exception) {

            }
        }
    }
}