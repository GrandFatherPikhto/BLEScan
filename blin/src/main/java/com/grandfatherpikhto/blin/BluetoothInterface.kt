package com.grandfatherpikhto.blin

import android.bluetooth.*
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.bluetooth.BluetoothDevice.BOND_BONDING
import android.util.Log
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BluetoothInterface {
    companion object Instance {
        private var instance: BluetoothInterface? = null
        const val TAG:String = "BluetoothInterface"
        fun getInstance(): BluetoothInterface {
            instance?.let {
                return instance!!
            }
            instance = BluetoothInterface()
            return instance!!
        }
    }

    operator fun getValue(
        owner: Any?,
        property: KProperty<*>
    ): BluetoothInterface {
        return getInstance()
    }

    /** */
    // private val bluetoothListener: MutableList<WeakReference<BluetoothListener>> = mutableListOf()
    private val bluetoothListener: MutableList<BluetoothListener> = mutableListOf()

    /** */
    var bluetoothAdapter:BluetoothAdapter? by Delegates.observable(null) {_, _, newValue ->
        Log.e(TAG, "bluetoothListener size: ${bluetoothListener.size}")
        newValue?.let {
            bluetoothEnabled = it.isEnabled
        }
        bluetoothListener.forEach { listener ->
            listener.onSetBluetoothAdapter(newValue)
        }
    }

    /** */
    var bluetoothPairing:BluetoothDevice? by Delegates.observable(null) { _, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onBluetoothPairingRequest(oldValue, newValue)
        }
    }

    /** */
    var bluetoothDevice:BluetoothDevice? by Delegates.observable(null) { _, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onSetBluetoothDevice(oldValue, newValue)
        }
    }

    /** */
    var currentDevice: BluetoothDevice? by Delegates.observable(null) { _, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onSetCurrentDevice(oldValue, newValue)
        }
    }

    /** */
    var btLeInterface:BtLeInterface? by Delegates.observable(null) { property, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onBtLeInterfaceBound(oldValue, newValue)
        }
    }

    /** */
    var bluetoothEnabled:Boolean by Delegates.observable(false) { _, _, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onBluetoothEnabled(newValue)
        }
    }

    /** */
    var bluetoothGatt:BluetoothGatt? by Delegates.observable(null) { _, _, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onGattChanged(newValue)
        }
    }

    /** */
    var scannerState: BtLeScanner.State by Delegates.observable(BtLeScanner.State.Unknown) { _, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onChangeScannerState(oldValue, newValue)
        }
    }

    /** */
    var connectorState: BtLeConnector.State by Delegates.observable(BtLeConnector.State.Unknown) { _, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onChangeConnectorState(oldValue, newValue)
        }
    }

    /** */
    var deviceFound:BluetoothDevice? by Delegates.observable(null) { _, _, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onFindDevice(newValue)
        }
    }

    /** */
    var scanError:Int by Delegates.observable(0) { _, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onScanError(oldValue, newValue)
        }
    }

    /** */
    var connectorError:Int by Delegates.observable(0) { _, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onGattError(oldValue, newValue)
        }
    }

    /** */
    fun changeBluetoothState(bluetoothDevice: BluetoothDevice?, oldValue:Int, newValue: Int) {
        when(newValue) {
            BluetoothAdapter.STATE_ON -> bluetoothEnabled = true
            BluetoothAdapter.STATE_OFF -> bluetoothEnabled = false
            else -> {  }
        }
        bluetoothListener.forEach { callback ->
            callback.onChangeBluetoothState(bluetoothDevice, oldValue, newValue)
        }
    }

    /** */
    fun changeBluetoothBondState(bluetoothDevice: BluetoothDevice?, oldValue: Int, newValue: Int) {
        bluetoothPairing?.let { pairing ->
            if ( bluetoothDevice != null
                && pairing.address == bluetoothDevice.address
                && oldValue == BluetoothDevice.BOND_BONDING
                && newValue == BluetoothDevice.BOND_BONDED ) {
                bluetoothPairing = pairing
            }
            bluetoothListener.forEach { listener ->
                listener.onBluetoothPaired(pairing)
            }
        }

        bluetoothListener.forEach { callback ->
            callback.onChangeBluetoothBondState(bluetoothDevice, oldValue, newValue)
        }
    }

    /** */
    fun characteristicRead(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?, state: Int) {
        bluetoothListener.forEach { listener ->
            listener.onCharacteristicReaded(bluetoothGatt, bluetoothGattCharacteristic, state)
        }
    }

    /** */
    fun characteristicWrite(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?, state: Int) {
        bluetoothListener.forEach { listener ->
            listener.onCharacteristicWrite(bluetoothGatt, bluetoothGattCharacteristic, state)
        }
    }

    /** */
    fun characteristicChange(bluetoothGatt: BluetoothGatt?, bluetoothGattCharacteristic: BluetoothGattCharacteristic?) {
        bluetoothListener.forEach { listener ->
            listener.onCharacteristicChanged(bluetoothGatt, bluetoothGattCharacteristic)
        }
    }

    /** */
    fun descriptorRead(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state:Int) {
        bluetoothListener.forEach { listener ->
            listener.onDescriptorReaded(bluetoothGatt, bluetoothGattDescriptor, state)
        }
    }

    /** */
    fun descriptorWrite(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state: Int) {
        bluetoothListener.forEach { listener ->
            listener.onDescriptorWrite(bluetoothGatt, bluetoothGattDescriptor, state)
        }
    }

    /** */
    fun addListener(listener: BluetoothListener) {
        bluetoothListener.add(listener)
    }

    fun removeListener(listener: BluetoothListener): Boolean {
        return bluetoothListener.remove(listener)
    }

    fun leScanDevices(addresses: String? = null, names: String? = null, mode: BtLeScanner.Mode = BtLeScanner.Mode.FindAll) {
        Log.d(TAG, "leScanDevices $addresses, names $names, mode: $mode")
        btLeInterface?.scanLeDevices(addresses = addresses, names = names, mode = mode)
    }
    fun leScanDevices(addresses: Array<String> = arrayOf(), names: Array<String> = arrayOf(), mode: BtLeScanner.Mode = BtLeScanner.Mode.FindAll)
        = btLeInterface?.scanLeDevices(addresses = addresses, names = names, mode = mode)
    fun stopScan() = btLeInterface?.stopScan()
    fun pairedDevices() = btLeInterface?.pairedDevices()

    fun connect(address:String) = btLeInterface?.connect(address = address)
    fun connect(device: BluetoothDevice) = btLeInterface?.connect(device)
    fun close() = btLeInterface?.close()

    fun bluetoothDisable() {
        bluetoothAdapter?.disable()
    }

    /**
     *
     */
    fun requestCharacteristic(uuid: UUID): Boolean {
        bluetoothGatt?.let { gatt ->
            gatt.services.forEach { service ->
                service.characteristics.find { characteristic ->
                    val res = characteristic.uuid == uuid
                    if(res) {
                        return gatt.readCharacteristic(characteristic)
                    }
                    res
                }
            }
        }

        return false
    }

    /**
     *
     */
    fun requestDescriptor(charUuid: UUID, descrUuid: UUID): Boolean {
        bluetoothGatt?.let { gatt ->
            gatt.services.forEach { service ->
                service.characteristics.find { characteristic ->
                    val charRes = characteristic.uuid == charUuid
                    if(charRes) {
                        characteristic.descriptors.find { descriptor ->
                            val descrRes = descriptor.uuid == descrUuid
                            if(descrRes) {
                                return gatt.readDescriptor(descriptor)
                            }
                            descrRes
                        }
                    }
                    charRes
                }
            }
        }

        return false
    }
}