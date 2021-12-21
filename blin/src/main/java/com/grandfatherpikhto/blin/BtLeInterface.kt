package com.grandfatherpikhto.blin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeInterface(private val applicationContext: Context) {
    companion object {
        const val TAG:String = "BtLeInterface"
    }

    /** */
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()
    /** */

    /** */
    private lateinit var bluetoothManager: BluetoothManager
    /** */
    private lateinit var bluetoothAdapter: BluetoothAdapter
    /** */
    private lateinit var btLeConnector: BtLeConnector
    val connector: BtLeConnector get() = btLeConnector
    /** */
    private lateinit var btLeScanner: BtLeScanner
    val scanner get() = btLeScanner
    /** */
    private lateinit var bcReceiver: BcReceiver
    val receiver get() = bcReceiver
    /** */
    private lateinit var btOutputQueue: BtOutputQueue
    /** */
    private lateinit var btInputQueue: BtInputQueue
    /** */
    val context get() = applicationContext

    /**
     * TODO: Почему создание сервиса вызывется дважды/трижды?!
     * https://stackoverflow.com/questions/7211066/android-service-oncreate-is-called-multiple-times-without-calling-ondestroy
     */
    init {
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothInterface.bluetoothAdapter = bluetoothAdapter

        bcReceiver    = BcReceiver(this)
        btLeScanner   = BtLeScanner(this)
        btLeConnector = BtLeConnector(this)
        btOutputQueue  = BtOutputQueue(this)
        btInputQueue = BtInputQueue(this)

        context.registerReceiver(bcReceiver, makeIntentFilter())

        bluetoothInterface.btLeInterface = this
        Log.d(TAG, "Init")
    }

    /**
     *
     */
    fun destroy() {
        Log.d(TAG, "onDestroy()")

        btLeConnector.destroy()
        btLeScanner.destroy()
        btOutputQueue.destroy()
        btInputQueue.destroy()
        bcReceiver.destroy()

        context.unregisterReceiver(bcReceiver)
    }

    fun scanLeDevices(addresses: String? = null,
                      names: String? = null,
                      mode: BtLeScanner.Mode = BtLeScanner.Mode.FindAll) {
        btLeConnector.close()
        btLeScanner.scanLeDevices(addresses, names, mode)
    }

    fun scanLeDevices( addresses: Array<String> = arrayOf<String>(),
                       names: Array<String> = arrayOf<String>(),
                       mode: BtLeScanner.Mode = BtLeScanner.Mode.FindAll)
            = btLeScanner.scanLeDevices(addresses, names, mode)

    fun stopScan() = btLeScanner.stopScan()

    fun pairedDevices() = btLeScanner.pairedDevices()

    fun connect(device: BluetoothDevice) {
        btLeScanner.stopScan()
        btLeConnector.connect(device)
    }

    fun connect(address:String) {
        btLeScanner.stopScan()
        btLeConnector.connect(address)
    }

    fun close() {
        btLeConnector.close()
    }

    fun writeCharacteristic(uuid: UUID, value: ByteArray, last:Boolean = false) = btOutputQueue.writeCharacteristic(uuid, value, last)
    fun writeDescriptor(charUuid: UUID, descrUUID: UUID, value: ByteArray, last:Boolean = false) = btOutputQueue.writeDescriptor(charUuid, descrUUID, value, last)
    fun readCharacteristic(uuid: UUID, last:Boolean = false) = btInputQueue.readCharacteristic(uuid, last)
    fun readDescriptor(charUuid: UUID, descrUuid: UUID, last:Boolean = false) = btInputQueue.readDescriptor(charUuid, descrUuid, last)

    /**
     * Создаём фильтр перехвата для различных широковещательных событий
     * В данном случае, нужны только фильтры для перехвата
     * В данном случае, нужны только фильтры для перехвата
     * запроса на сопряжение устройства и завершения сопряжения
     * И интересует момент "Устройство найдено" на случай рескана устройств
     * по адресу или имени
     */
    private fun makeIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()

        intentFilter.addAction(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)

        return intentFilter
    }
}