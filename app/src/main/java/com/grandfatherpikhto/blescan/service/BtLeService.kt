package com.grandfatherpikhto.blescan.service

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.grandfatherpikhto.blescan.model.BtLeDevice
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlin.properties.Delegates

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeService: Service() {
    /** */
    companion object {
        const val TAG:String = "BtLeService"
    }

    /** */
    private val bluetoothInterface:BluetoothInterface by BluetoothInterfaceLazy()
    /** */

    /** */
    private lateinit var bluetoothManager:BluetoothManager
    /** */
    private lateinit var bluetoothAdapter:BluetoothAdapter
    /** */
    private lateinit var btLeConnector:BtLeConnector
    val connector: BtLeConnector get() = btLeConnector
    /** */
    private lateinit var btLeScanner:BtLeScanner
    val scanner get() = btLeScanner
    /** */
    private lateinit var bcReceiver:BcReceiver
    val receiver get() = bcReceiver
    /** */
    private lateinit var btCharIO: BtCharIO

    /** Binder given to clients */
    private val binder = LocalBinder()
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        fun getService(): BtLeService = this@BtLeService
    }

    /**
     *
     */
    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG, "Сервис связан")

        return binder
    }

    /**
     *
     */
    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    init {
        Log.d(TAG, "Init")
    }

    /**
     * TODO: Почему создание сервиса вызывется дважды/трижды?!
     * https://stackoverflow.com/questions/7211066/android-service-oncreate-is-called-multiple-times-without-calling-ondestroy
     */
    override fun onCreate() {
        super.onCreate()

        bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothInterface.bluetoothAdapter = bluetoothAdapter

        bcReceiver    = BcReceiver(this)
        btLeScanner   = BtLeScanner(this)
        btLeConnector = BtLeConnector(this)
        btCharIO      = BtCharIO(this)

        applicationContext.registerReceiver(bcReceiver, makeIntentFilter())

        Log.d(TAG, "onCreate()")
    }

    /**
     *
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")

        btLeConnector.destroy()
        btLeScanner.destroy()
        bcReceiver.destroy()
        btCharIO.destroy()

        applicationContext.unregisterReceiver(bcReceiver)
    }

    fun scanLeDevices( addresses: String? = null,
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

    fun connect(btLeDevice: BtLeDevice) {
        btLeScanner.stopScan()
        btLeConnector.connect(btLeDevice)
    }

    fun connect(address:String) {
        btLeScanner.stopScan()
        btLeConnector.connect(address)
    }

    fun close() {
        btLeConnector.close()
    }

    fun writeCharacteristic(uuid:String, value:ByteArray) = btCharIO.writeCharacteristic(uuid, value)
    fun writeDescriptor(uuid: String, value: ByteArray) = btCharIO.writeDescriptor(uuid, value)

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


