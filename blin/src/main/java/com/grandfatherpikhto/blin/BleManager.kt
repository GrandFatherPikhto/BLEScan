package com.grandfatherpikhto.blin

import android.bluetooth.*
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.grandfatherpikhto.blin.buffer.BleCharacteristicNotify
import com.grandfatherpikhto.blin.data.BleGattItem
import com.grandfatherpikhto.blin.data.BleBondState
import com.grandfatherpikhto.blin.data.BleGatt
import com.grandfatherpikhto.blin.data.BleScanResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class BleManager constructor(private val context: Context,
                             dispatcher: CoroutineDispatcher = Dispatchers.IO)
    : BleManagerInterface {

    private val logTag = this.javaClass.simpleName
    private val scope = CoroutineScope(dispatcher)

    val applicationContext:Context get() = context.applicationContext

    val bleScanManager: BleScanManager = BleScanManager(context, dispatcher)
    val bleGattManager: BleGattManager = BleGattManager(context, bleScanManager, dispatcher)
    val bleBondManager: BleBondManager = BleBondManager(context, dispatcher)

    override val stateFlowScanState get() = bleScanManager.stateFlowScanState
    override val scanState get()     = bleScanManager.scanState

    override val sharedFlowBleScanResult get() = bleScanManager.sharedFlowBleScanResult

    override val scanResults get() = bleScanManager.scanResults.map { BleScanResult(it) }

    override val stateFlowScanError get() = bleScanManager.stateFlowError
    override val scanError get()     = bleScanManager.scanError

    override val stateFlowConnectState get() = bleGattManager.stateFlowConnectState
    override val connectState get() = bleGattManager.connectState

    override val sharedFlowConnectStateCode get() = bleGattManager.stateFlowConnectStateCode

    override val stateFlowBleGatt get() = bleGattManager.stateFlowBleGatt
    override val bleGatt get() = bleGattManager.bleGatt

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

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        owner.lifecycle.addObserver(bleScanManager)
        owner.lifecycle.addObserver(bleGattManager)
        owner.lifecycle.addObserver(bleBondManager)
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

    override fun connect(address: String): BleGatt? {
        bleGattManager.connect(address)?.let {
            return BleGatt(it)
        }

        return null
    }

    override fun disconnect() = bleGattManager.disconnect()

    override fun writeGattData(bleGattData: BleGattItem) = bleGattManager.writeGattData(bleGattData)

    override fun readCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic)
        = bleGattManager.readCharacteristic(bluetoothGattCharacteristic)

    override fun readGattData(bleGattData: BleGattItem): Boolean =
        bleGattManager.readGattData(bleGattData)

    override fun readDescriptor(bluetoothGattDescriptor: BluetoothGattDescriptor)
        = bleGattManager.readDescriptor(bluetoothGattDescriptor)

    override fun notifyCharacteristic(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic)
        = bleGattManager.notifyCharacteristic(bluetoothGattCharacteristic)

    override fun notifyCharacteristic(bleGattData: BleGattItem) =
        bleGattManager.notifyCharacteristic(bleGattData)

    override fun isCharacteristicNotified(
        bluetoothGattCharacteristic: BluetoothGattCharacteristic): Boolean
        = bleGattManager.isCharacteristicNotified(bluetoothGattCharacteristic)
}