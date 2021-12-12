package com.grandfatherpikhto.blescan.service

import android.bluetooth.*
import android.util.Log
import com.grandfatherpikhto.blescan.model.BtLeDevice
import com.grandfatherpikhto.blescan.model.toBtLeDevice
import kotlin.properties.Delegates
import kotlin.reflect.KProperty
import java.lang.ref.WeakReference

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BluetoothInterface {
    companion object Instance {
        private var instance: BluetoothInterface? = null
        const val TAG:String = "BluetoothInterface"
        fun getInstance():BluetoothInterface {
            instance?.let {
                return instance!!
            }
            instance = BluetoothInterface()
            return instance!!
        }
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
    var currentDevice: BtLeDevice? by Delegates.observable(null) { _, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onSetCurrentDevice(oldValue, newValue)
        }
    }

    /** */
    var service:BtLeService? by Delegates.observable(null) {property, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onServiceBound(oldValue, newValue)
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
    var scannerState:BtLeScanner.State by Delegates.observable(BtLeScanner.State.Unknown) { _, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onChangeScannerState(oldValue, newValue)
        }
    }

    /** */
    var connectorState:BtLeConnector.State by Delegates.observable(BtLeConnector.State.Unknown) { _, oldValue, newValue ->
        bluetoothListener.forEach { listener ->
            listener.onChangeConnectorState(oldValue, newValue)
        }
    }

    /** */
    var deviceFound:BtLeDevice? by Delegates.observable(null) { _, _, newValue ->
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
                listener.onBluetoothPaired(pairing.toBtLeDevice())
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
            listener.onCharacteristicWrited(bluetoothGatt, bluetoothGattCharacteristic, state)
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
    fun descriptoWrite(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor?, state: Int) {
        bluetoothListener.forEach { listener ->
            listener.onDescriptorWrited(bluetoothGatt, bluetoothGattDescriptor, state)
        }
    }

    /** */
    fun descriptorWrite(bluetoothGatt: BluetoothGatt?, bluetoothGattDescriptor: BluetoothGattDescriptor, state:Int) {
        bluetoothListener.forEach { listener ->
            listener.onDescriptorWrited(bluetoothGatt, bluetoothGattDescriptor, state)
        }
    }


    /** */
    fun addListener(listener: BluetoothListener) {
        bluetoothListener.add(listener)
    }

    fun removeListener(listener: BluetoothListener): Boolean {
        return bluetoothListener.remove(listener)
    }

    operator fun getValue(
        owner: Any?,
        property: KProperty<*>
    ): BluetoothInterface {
        return getInstance()
    }

    fun leScanDevices(addresses: String? = null, names: String? = null, mode: BtLeScanner.Mode = BtLeScanner.Mode.FindAll)
        = service?.scanLeDevices(addresses = addresses, names = names, mode = mode)
    fun leScanDevices(addresses: Array<String> = arrayOf(), names: Array<String> = arrayOf(), mode: BtLeScanner.Mode = BtLeScanner.Mode.FindAll)
        = service?.scanLeDevices(addresses = addresses, names = names, mode = mode)
    fun stopScan() = service?.stopScan()
    fun pairedDevices() = service?.pairedDevices()

    fun connect(address:String) = service?.connect(address = address)
    fun connect(btLeDevice: BtLeDevice) = service?.connect(btLeDevice)
    fun close() = service?.close()

    fun bluetoothDisable() {
        bluetoothAdapter?.disable()
    }
}