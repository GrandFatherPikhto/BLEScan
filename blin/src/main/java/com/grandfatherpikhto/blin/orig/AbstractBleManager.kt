package com.grandfatherpikhto.blin.orig

import android.bluetooth.*
import android.content.Context
import com.grandfatherpikhto.blin.BleBondManager
import com.grandfatherpikhto.blin.BleGattManager
import com.grandfatherpikhto.blin.BleManagerInterface
import com.grandfatherpikhto.blin.BleScanManager
import com.grandfatherpikhto.blin.buffer.BleCharacteristicNotify
import com.grandfatherpikhto.blin.data.BleGattItem
import com.grandfatherpikhto.blin.data.BleBondState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

abstract class AbstractBleManager constructor(private val context: Context,
                                              dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : BleManagerInterface {

    private val logTag = this.javaClass.simpleName
    val scope = CoroutineScope(dispatcher)

    val applicationContext:Context get() = context.applicationContext

    override val bleScanManager: AbstractBleScanManager = BleScanManager(context, dispatcher)
    override val bleGattManager: AbstractBleGattManager by lazy {
        BleGattManager(context, bleScanManager, dispatcher) }
    override val bleBondManager: AbstractBleBondManager = BleBondManager(context, dispatcher)

    override val stateFlowScanState get() = bleScanManager.stateFlowScanState
    override val scanState get()     = bleScanManager.scanState

    override val sharedFlowScanResults get() = bleScanManager.sharedFlowBleScanResult

    override val scanResults get() = bleScanManager.scanResults

    override val stateFlowScanError get() = bleScanManager.stateFlowError
    override val scanError get()     = bleScanManager.scanError

    override val stateFlowConnectState get() = bleGattManager.stateFlowConnectState
    override val connectState get() = bleGattManager.connectState

    override val sharedFlowConnectStateCode get() = bleGattManager.stateFlowConnectStateCode

    override val stateFlowBluetoothGatt get() = bleGattManager.stateFlowBluetoothGatt
    override val bluetoothGatt get() = bleGattManager.bluetoothGatt

    override val sharedFlowCharacteristic: SharedFlow<BluetoothGattCharacteristic>
        get() = bleGattManager.sharedFlowCharacteristic
    override val sharedFlowDescriptor: SharedFlow<BluetoothGattDescriptor>
        get() = bleGattManager.sharedFlowDescriptor

    override val stateFlowBondState get() = bleBondManager.stateFlowBondState
    override val bondState: BleBondState? get() = bleBondManager.bondState

    override val sharedFlowCharacteristicNotify: SharedFlow<BleCharacteristicNotify>
        get() = bleGattManager.sharedFlowCharacteristicNotify
    override val notifiedCharacteristic: List<BluetoothGattCharacteristic>
        get() = bleGattManager.notifiedCharacteristic

    override fun bondRequest(address: String)
            = bleBondManager.bondRequest(address)

    init {
    }

    override fun onDestroy() {
        bleScanManager.onDestroy()
        bleGattManager.onDestroy()
        bleBondManager.onDestroy()
    }

    override
    fun startScan(addresses: List<String>,
                  names: List<String>,
                  services: List<String>,
                  stopOnFind: Boolean,
                  filterRepeatable: Boolean,
                  stopTimeout: Long
    ) : Boolean = bleScanManager.startScan( addresses, names, services,
        stopOnFind, filterRepeatable, stopTimeout )

    override fun stopScan() = bleScanManager.stopScan()

    override fun connect(address: String): BluetoothGatt? = bleGattManager.connect(address)

    override fun disconnect() = bleGattManager.disconnect()

    override fun addGattData(bleGattData: BleGattItem) = bleGattManager.addGattData(bleGattData)

    override fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic)
        = bleGattManager.readCharacteristic(bluetoothGattCharacteristic)

    override fun notifyCharacteristic(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic)
        = bleGattManager.notifyCharacteristic(bluetoothGattCharacteristic)

    override fun notifyCharacteristic(bleGattData: BleGattItem) =
        bleGattManager.notifyCharacteristic(bleGattData)

    override fun isCharacteristicNotified(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic): Boolean
        = bleGattManager.isCharacteristicNotified(bluetoothGattCharacteristic)

    override val isBluetoothAdapterEnabled: Boolean get() = (applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter.isEnabled
}