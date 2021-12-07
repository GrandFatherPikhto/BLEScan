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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtLeService: Service() {
    /** */
    companion object {
        const val TAG:String = "BtLeService"
    }

    /** */
    enum class State(val value:Int) {
        None(0x00),
        Scanning(0x01),
        Stopped(0x02),
        Disconnecting(0x04),
        Disconnected(0x05),
        Connecting(0x06),
        Connected(0x07),
        Error(0xFE),
        FatalError(0xFF)
    }

    /** */
    private lateinit var bluetoothManager: BluetoothManager

    val btManager get() = bluetoothManager

    /** */
    private lateinit var bluetoothAdapter: BluetoothAdapter
    /** */
    val adapter get() = bluetoothAdapter

    private lateinit var btLeConnector:BtLeConnector
    val connector get() = btLeConnector

    private lateinit var btLeScanner:BtLeScanner
    val scanner get() = btLeScanner

    private lateinit var bcReceiver:BcReceiver
    val receiver get() = bcReceiver

    /** */
    private val sharedState = MutableStateFlow(State.None)
    val state = sharedState.asStateFlow()

    /** */
    private val sharedDevice = MutableSharedFlow<BtLeDevice>(replay = 10)
    val device = sharedDevice.asSharedFlow()

    /** */
    private val sharedGatt = MutableStateFlow<BluetoothGatt?>(null)
    val gatt = sharedGatt.asStateFlow()

    /** */
    private val sharedBtEnabled = MutableStateFlow<Boolean>(false)
    val enabled = sharedBtEnabled.asStateFlow()


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
        sharedState.tryEmit(State.Disconnected)
        return binder
    }

    /**
     *
     */
    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    /**
     *
     */
    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        super.onCreate()
        bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        sharedBtEnabled.value = bluetoothAdapter.isEnabled


        bcReceiver    = BcReceiver(this)
        btLeScanner   = BtLeScanner(this)
        btLeConnector = BtLeConnector(this)

        applicationContext.registerReceiver(bcReceiver, makeIntentFilter())

        btLeConnector.addEventListener(object: BtLeConnector.ConnectorCallback {
            override fun onChangeGattState(connectorState: BtLeConnector.State) {
                when(connectorState) {
                    BtLeConnector.State.Disconnecting -> { sharedState.tryEmit(State.Disconnecting) }
                    BtLeConnector.State.Disconnected  -> { sharedState.tryEmit(State.Disconnected) }
                    BtLeConnector.State.Connecting    -> { sharedState.tryEmit(State.Connecting) }
                    BtLeConnector.State.Connected     -> { sharedState.tryEmit(State.Connected) }
                }
            }

            override fun onGattDiscovered(bluetoothGatt: BluetoothGatt) {
                sharedGatt.tryEmit(bluetoothGatt)
            }
        })

        btLeScanner.addEventListener(object: BtLeScanner.ScannerCallback {
            override fun onChangeState(scannerState: BtLeScanner.State) {
                when(scannerState) {
                    BtLeScanner.State.Scanning -> { sharedState.tryEmit(State.Scanning) }
                    BtLeScanner.State.Stopped  -> { sharedState.tryEmit(State.Stopped) }
                    BtLeScanner.State.Error    -> { sharedState.tryEmit(State.Error)}
                }
            }

            override fun onFindDevice(btLeDevice: BtLeDevice?) {
                super.onFindDevice(btLeDevice)
                btLeDevice?.let { found ->
                    sharedDevice.tryEmit(found)
                }
            }
        })

        receiver.addEventListener(object: BcReceiver.ReceiverCallback {
            override fun onBluetoothEnable(enable: Boolean) {
                super.onBluetoothEnable(enable)
                sharedBtEnabled.tryEmit(enable)
            }
        })
    }

    /**
     *
     */
    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        applicationContext.unregisterReceiver(bcReceiver)
        super.onDestroy()
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