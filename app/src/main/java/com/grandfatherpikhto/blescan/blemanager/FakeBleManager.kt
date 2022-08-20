package com.grandfatherpikhto.blescan.blemanager

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.grandfatherpikhto.blin.orig.AbstractBleGattManager
import com.grandfatherpikhto.blin.orig.AbstractBleScanManager
import com.grandfatherpikhto.blin.buffer.BleCharacteristicNotify
import com.grandfatherpikhto.blin.data.BleGattItem
import com.grandfatherpikhto.blin.data.*
import com.grandfatherpikhto.blin.idling.ConnectingIdling
import com.grandfatherpikhto.blin.idling.DisconnectingIdling
import com.grandfatherpikhto.blin.idling.ScanIdling
import com.grandfatherpikhto.blescan.data.BleDevice
import com.grandfatherpikhto.blescan.data.BleGatt
import com.grandfatherpikhto.blescan.data.BleScanResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.random.Random

class FakeBleManager(context: Context) : AppBleManager (context) {

    private val logTag = this.javaClass.simpleName

    private val mutableStateFlowScanState = MutableStateFlow(AbstractBleScanManager.State.Stopped)
    override val stateFlowScanState: StateFlow<AbstractBleScanManager.State>
        get() = mutableStateFlowScanState.asStateFlow()
    override val scanState: AbstractBleScanManager.State
        get() = mutableStateFlowScanState.value

    private val mutableBleScanResults = mutableListOf<BleScanResult>()

    private val mutableFlowScanError = MutableStateFlow(-1)
    override val stateFlowScanError: StateFlow<Int>
        get() = mutableFlowScanError.asStateFlow()
    override val scanError: Int
        get() = mutableFlowScanError.value

    private val mutableStateFlowConnectState = MutableStateFlow(AbstractBleGattManager.State.Disconnected)
    override val stateFlowConnectState: StateFlow<AbstractBleGattManager.State>
        get() = mutableStateFlowConnectState.asStateFlow()
    override val connectState: AbstractBleGattManager.State
        get() = mutableStateFlowConnectState.value

    private val mutableSharedFlowStateCode = MutableStateFlow(-1)
    override val sharedFlowConnectStateCode: SharedFlow<Int>
        get() = mutableSharedFlowStateCode.asStateFlow()

    private val mutableStateFlowBluetoothGatt = MutableStateFlow<BluetoothGatt?>(null)
    override val stateFlowBluetoothGatt: StateFlow<BluetoothGatt?>
        get() = mutableStateFlowBluetoothGatt.asStateFlow()
    override val bluetoothGatt: BluetoothGatt?
        get() = mutableStateFlowBluetoothGatt.value

    private val mutableSharedFlowCharacteristic = MutableSharedFlow<BluetoothGattCharacteristic>(replay = 100)
    override val sharedFlowCharacteristic: SharedFlow<BluetoothGattCharacteristic>
        get() = mutableSharedFlowCharacteristic.asSharedFlow()

    private val mutableSharedFlowDescriptor = MutableSharedFlow<BluetoothGattDescriptor>(replay = 100)
    override val sharedFlowDescriptor: SharedFlow<BluetoothGattDescriptor>
        get() = mutableSharedFlowDescriptor.asSharedFlow()

    private val mutableStateFlowBond = MutableStateFlow<BleBondState?>(null)
    override val stateFlowBondState: StateFlow<BleBondState?>
        get() = mutableStateFlowBond.asStateFlow()
    override val bondState: BleBondState?
        get() = mutableStateFlowBond.value

    private val mutableSharedFlowNotifyCharacteristic = MutableSharedFlow<BleCharacteristicNotify>(replay = 100)
    override val notifiedCharacteristic: List<BluetoothGattCharacteristic>
        get() = TODO("Not yet implemented")
    override val sharedFlowCharacteristicNotify: SharedFlow<BleCharacteristicNotify>
        get() = mutableSharedFlowNotifyCharacteristic.asSharedFlow()

    override fun bondRequest(address: String): Boolean {
        TODO("Not yet implemented")
    }

    val scanIdling: ScanIdling = ScanIdling.getInstance(this)
    val connectingIdling: ConnectingIdling = ConnectingIdling.getInstance(this)
    val disconnectingIdling: DisconnectingIdling = DisconnectingIdling.getInstance(this)

    override fun startScan(
        addresses: List<String>,
        names: List<String>,
        services: List<String>,
        stopOnFind: Boolean,
        filterRepeatable: Boolean,
        stopTimeout: Long
    ): Boolean {
        Log.d(logTag, "startScan($scanState)")
        if (scanState != AbstractBleScanManager.State.Scanning) {
            mutableStateFlowScanState.tryEmit(AbstractBleScanManager.State.Scanning)
            scope.launch {
                (1..10).forEach { i ->
                    delay(Random.nextLong(200, 1000))
                    mutableSharedFlowBleScanResult.tryEmit(
                        BleScanResult(
                            BleDevice(Random.nextBytes(6)
                                .joinToString (":") { String.format("%02X", it) },
                                String.format("BLE_%02d", i),
                                if (Random.nextBoolean()) BluetoothDevice.BOND_BONDED else BluetoothDevice.BOND_NONE),
                            Random.nextBoolean(),
                            Random.nextInt(-100, 0)))
                    if (scanState != AbstractBleScanManager.State.Scanning) return@forEach
                }
                stopScan()
            }
        }
        return true
    }

    init {
        scope.launch {
            sharedFlowBleScanResult.collect {
                mutableBleScanResults.add(it)
            }
        }
    }


    override fun stopScan() {
        Log.d(logTag, "stopScan()")
        mutableStateFlowScanState.tryEmit(AbstractBleScanManager.State.Stopped)
    }

    private fun generateRandomBle(bleDevice: BleDevice): BleGatt {
        val services = mutableListOf<BluetoothGattService>()
        (0..1).forEach { _ ->
            val service = BluetoothGattService(UUID.randomUUID(), 0)
            (0..Random.nextInt(1,5)).forEach { _ ->
                val characteristic = BluetoothGattCharacteristic(UUID.randomUUID(), 0, 0)
                characteristic.value = Random.nextBytes(Random.nextInt(1,10))
                (0..1).forEach { _ ->
                    val descriptor = BluetoothGattDescriptor(UUID.randomUUID(), 0)
                    descriptor.value = ByteArray(1) {
                        Random.nextInt(0, 2).toByte() }
                    characteristic.addDescriptor(descriptor)
                }
                service.addCharacteristic(characteristic)
            }
            services.add(service)
        }
        return BleGatt(bleDevice, services)
    }

    override fun connectBle(address: String): BleGatt? {
        Log.d(logTag, "connect($address)")
        bleScanResults.find { it.device.address == address }?.let { scanResult ->
            val bleGatt = generateRandomBle(scanResult.device)
            mutableStateFlowConnectState.tryEmit(AbstractBleGattManager.State.Connected)
            // mutableStateFlowBluetoothGatt.tryEmit(bleGatt)
            scope.launch {
                mutableStateFlowConnectState.tryEmit(AbstractBleGattManager.State.Connecting)
                delay(Random.nextLong(300, 1500))
                mutableStateFlowConnectState.tryEmit(AbstractBleGattManager.State.Connected)
            }

            return bleGatt
        }

        return null
    }

    override fun disconnect() {
        Log.d(logTag, "disconnect()")
        scope.launch {
            mutableStateFlowConnectState.tryEmit(AbstractBleGattManager.State.Disconnecting)
            delay(Random.nextLong(100, 500))
            mutableStateFlowConnectState.tryEmit(AbstractBleGattManager.State.Disconnected)
            // mutableStateFlowBleGatt.tryEmit(null)
        }
    }

    override fun addGattData(bleGattData: BleGattItem) {

    }

    override fun notifyCharacteristic(bleGattData: BleGattItem) { }

    override fun notifyCharacteristic(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
    }

    override fun isCharacteristicNotified(bluetoothGattCharacteristic: BluetoothGattCharacteristic): Boolean {
        return true
    }

    override fun onDestroy() {

    }
}