package com.grandfatherpikhto.blin

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.grandfatherpikhto.blin.data.BleScanResult
import com.grandfatherpikhto.blin.idling.ScanIdling
import com.grandfatherpikhto.blin.receivers.BcScanReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class BleScanManager constructor(private val context: Context,
                                 ioDispatcher: CoroutineDispatcher = Dispatchers.IO)
    : DefaultLifecycleObserver {

    private val bcScanReceiver: BcScanReceiver = BcScanReceiver(this)

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter
            = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner
            = bluetoothAdapter.bluetoothLeScanner

    val applicationContext:Context get() = context.applicationContext

    enum class State (val value: Int) {
        Stopped(0x00),
        Scanning(0x01),
        Error(0x03)
    }

    private val tagLog = javaClass.simpleName

    private val mutableSharedFlowScanResult = MutableSharedFlow<ScanResult>(replay = 100)
    val sharedFlowScanResult:SharedFlow<ScanResult> get() = mutableSharedFlowScanResult.asSharedFlow()

    private val mutableSharedFlowBleScanResult = MutableSharedFlow<BleScanResult>(replay = 100)
    val sharedFlowBleScanResult get() = mutableSharedFlowBleScanResult.asSharedFlow()

    private val mutableStateFlowScanState = MutableStateFlow(State.Stopped)
    val stateFlowScanState get() = mutableStateFlowScanState.asStateFlow()
    val scanState get() = mutableStateFlowScanState.value

    private val mutableFlowStateError = MutableStateFlow(-1)
    val stateFlowError get() = mutableFlowStateError.asStateFlow()
    val scanError get() = mutableFlowStateError.value

    private var bleScanPendingIntent: PendingIntent = bcScanReceiver.pendingIntent

    private val scanFilters = mutableListOf<ScanFilter>()
    private val scanSettingsBuilder = ScanSettings.Builder()

    private var scope = CoroutineScope(ioDispatcher)
    private var notEmitRepeat: Boolean = true
    val scanResults = mutableListOf<ScanResult>()

    private var stopOnFind = false
    private var stopTimeout = 0L

    private val addresses = mutableListOf<String>()
    private val names = mutableListOf<String>()
    private val uuids = mutableListOf<ParcelUuid>()

    private var scanIdling: ScanIdling? = null

    init {
        initScanSettings()
        initScanFilters()
    }

    @SuppressLint("MissingPermission")
    fun startScan(addresses: List<String> = listOf(),
                  names: List<String> = listOf(),
                  services: List<String> = listOf(),
                  stopOnFind: Boolean = false,
                  filterRepeatable: Boolean = false,
                  stopTimeout: Long = 0L
    ) : Boolean {

        if (scanState == State.Error) {
            Log.e(tagLog, "Error: ${stateFlowError.value}")
            mutableStateFlowScanState.tryEmit(State.Stopped)
        }

        scanIdling?.idling = false

        if (scanState == State.Stopped) {

            scanResults.clear()

            this.addresses.clear()
            this.addresses.addAll(addresses)

            this.names.clear()
            this.names.addAll(names)

            this.stopOnFind = stopOnFind
            this.notEmitRepeat = filterRepeatable

            this.uuids.clear()
            this.uuids.addAll(services.mapNotNull { ParcelUuid.fromString(it) }
                .toMutableList())

            Log.d(tagLog, "startScan(addresses = $addresses, names = $names, uuids = $uuids, " +
                    "stopOnFind = $stopOnFind, notEmitRepeat = $notEmitRepeat, stopTimeout = $stopTimeout)")

            if (stopTimeout > 0) {
                scope.launch {
                    this@BleScanManager.stopTimeout = stopTimeout
                    delay(stopTimeout)
                    stopScan()
                }
            }

            val result = bluetoothLeScanner.startScan(
                scanFilters,
                scanSettingsBuilder.build(),
                bleScanPendingIntent
            )
            if (result == 0) {
                mutableStateFlowScanState.tryEmit(State.Scanning)
                return true
            } else {
                mutableStateFlowScanState.tryEmit(State.Error)
            }
        }

        return false
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (scanState == State.Scanning) {
            Log.d(tagLog, "stopScan()")
            bluetoothLeScanner.stopScan(bleScanPendingIntent)
            mutableStateFlowScanState.tryEmit(State.Stopped)
        }
    }


    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Log.d(tagLog, "onCreate()")
        applicationContext.registerReceiver(bcScanReceiver, makeIntentFilters())
    }

    override fun onDestroy(owner: LifecycleOwner) {
        applicationContext.unregisterReceiver(bcScanReceiver)
        stopScan()
        super.onDestroy(owner)
    }

    private fun initScanFilters() {
        val filter = ScanFilter.Builder().build()
        scanFilters.add(filter)
    }

    @SuppressLint("MissingPermission")
    private fun filterName(bluetoothDevice: BluetoothDevice) : Boolean =
        names.isEmpty()
            .or(names.isNotEmpty()
                .and(bluetoothDevice.name != null)
                .and(names.contains(bluetoothDevice.name)))

    private fun filterAddress(bluetoothDevice: BluetoothDevice) : Boolean =
        addresses.isEmpty()
            .or(addresses.isNotEmpty().and(addresses.contains(bluetoothDevice.address)))

    private fun filterUuids(uuids: Array<ParcelUuid>?) : Boolean {
        if (this.uuids.isEmpty()) return true
        if (uuids.isNullOrEmpty()) return false
        if (this.uuids.containsAll(uuids.toList())) return true
        return false
    }

    fun onReceiveError(errorCode: Int) {
        if (errorCode > 0) {
            mutableFlowStateError.tryEmit(errorCode)
            stopScan()
        }
    }

    private fun isEmitScanResult(scanResult: ScanResult) : Boolean {
        val contains = scanResults
            .find { it.device.address == scanResult.device.address } != null
        if (contains) {
            val index = scanResults.indexOfFirst {
                it.device.address == scanResult.device.address }
            if (index >= 0) {
                scanResults[index] = scanResult
            }
        } else {
            scanResults.add(scanResult)
        }
        return !notEmitRepeat || !contains
    }

    @SuppressLint("MissingPermission")
    fun onReceiveScanResult(scanResult: ScanResult) {
        scanResult.device.let { bluetoothDevice ->
            if ( filterName(bluetoothDevice)
                .and(filterAddress(bluetoothDevice))
                .and(filterUuids(bluetoothDevice.uuids))
            ) {
                if (isEmitScanResult(scanResult)) {
                    mutableSharedFlowScanResult.tryEmit(scanResult)
                    mutableSharedFlowBleScanResult.tryEmit(BleScanResult(scanResult))
                }
                if (stopOnFind &&
                    (names.isNotEmpty()
                    .or(addresses.isNotEmpty()
                    .or(uuids.isNotEmpty())))
                ) {
                    stopScan()
                }
            }
        }
    }

    private fun makeIntentFilters() : IntentFilter = IntentFilter().let { intentFilter ->
        intentFilter.addAction(Intent.CATEGORY_DEFAULT)
        intentFilter.addAction(BcScanReceiver.ACTION_BLE_SCAN)
        intentFilter
    }

    private fun initScanSettings() {
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        scanSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        // setReportDelay() -- отсутствует. Не вызывать! Ответ приходит ПУСТОЙ!
        // В официальной документации scanSettingsBuilder.setReportDelay(1000)
        scanSettingsBuilder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
        scanSettingsBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        scanSettingsBuilder.setLegacy(false)
        scanSettingsBuilder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
    }
}