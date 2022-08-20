package com.grandfatherpikhto.blescan.ui.fragments.adapters

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blin.buffer.BleCharacteristicNotify
import com.grandfatherpikhto.blescan.data.BleGatt
import com.grandfatherpikhto.blescan.data.BleItem
import kotlin.properties.Delegates

class RvBleDeviceAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder> () {
    private val mutableListItems = mutableListOf<BleItem>()
    private val tagLog = this.javaClass.simpleName

    private var characteristicReadClickListener: ((BleItem, View) -> Unit)? = null
    private var characteristicNotifyClickListener: ((BleItem, View) -> Unit)? = null
    private var characteristicWriteClickListener: ((BleItem, View) -> Unit)? = null

    private var descriptorReadClickListener: ((BleItem, View) -> Unit)? = null

    enum class Format(val value: Int) {
        Bytes(R.drawable.ic_bytes),
        Text(R.drawable.ic_text),
        Integer(R.drawable.ic_integer),
        Float(R.drawable.ic_float);

        companion object {
            fun byResId(value: Int) = values().first { value == it.value }
        }
    }

    var bleGatt: BleGatt? by Delegates.observable(null) { _, _, value ->
        if (value == null) {
            clear()
        } else {
            setServices(value.services)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(BleItem.Type.byValue(viewType)) {
            BleItem.Type.Service -> {
                ServiceHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_service, parent, false))
            }
            BleItem.Type.Characteristic -> {
                CharacteristicHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_characteristic, parent, false))
            }
            BleItem.Type.Descriptor -> {
                DescriptorHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_descriptor, parent, false))
            }
            else -> { throw IllegalArgumentException("There is no such type $viewType") }
        }
    }

    private fun updateNestedItems(bleItemService: BleItem) =
        mutableListItems.forEachIndexed { position, bleItem ->
            if ((bleItem.type == BleItem.Type.Characteristic
                        || bleItem.type == BleItem.Type.Descriptor)
                && bleItem.uuidService == bleItemService.uuidService) {
                mutableListItems[position].opened = bleItemService.opened
                notifyItemChanged(position)
            }
        }

    private fun bindCharacteristicHolderListeners(holder: CharacteristicHolder) {
        holder.setOnCharacteristicReadClickListener { bleItem, view ->
            characteristicReadClickListener?.let { listener ->
                listener(bleItem, view)
            }
        }
        holder.setOnCharacteristicNotifyClickListener { bleItem, view ->
            characteristicNotifyClickListener?.let { listener ->
                listener(bleItem, view)
            }
        }
        holder.setOnCharacteristicWriteClickListener { bleItem, view ->
            characteristicWriteClickListener?.let { listener ->
                listener(bleItem, view)
            }
        }
    }

    private fun bindCharacteristicHolder(holder: CharacteristicHolder, position: Int) {
        holder.bind(mutableListItems[position])
        bindCharacteristicHolderListeners(holder)
    }

    private fun bindServiceHolder(holder: ServiceHolder, position: Int) {
        holder.bind(mutableListItems[position])
        holder.itemView.setOnClickListener { _ ->
            (mutableListItems[position]).inverseOpened()
            updateNestedItems(mutableListItems[position])
            notifyItemChanged(mutableListItems.indexOf(mutableListItems[position]))
        }
    }

    private fun bindDescriptorHolder(holder: DescriptorHolder, position: Int) {
        holder.bind(mutableListItems[position])
        holder.setOnDescriptorReadClickListener { bleItem, view ->
            descriptorReadClickListener?.let { listener ->
                listener(bleItem, view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (mutableListItems[position].type) {
            BleItem.Type.Service -> {
                bindServiceHolder(holder as ServiceHolder, position)
            }
            BleItem.Type.Characteristic -> {
                bindCharacteristicHolder(holder as CharacteristicHolder, position)
            }
            BleItem.Type.Descriptor -> {
                bindDescriptorHolder(holder as DescriptorHolder, position)
            }
            else -> {
                Log.d(tagLog, "Не пойми, что")
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        mutableListItems[position].type.value

    override fun getItemCount(): Int = mutableListItems.size

    private fun addService(bluetoothGattService: BluetoothGattService) {
        val bleItemService = BleItem(bluetoothGattService)
        mutableListItems.add(bleItemService)
        notifyItemInserted(mutableListItems.indexOf(bleItemService))
        bluetoothGattService.characteristics.forEach { bluetoothGattCharacteristic ->
            val bleItemCharacteristic = BleItem(bluetoothGattCharacteristic)
            mutableListItems.add(bleItemCharacteristic)
            notifyItemInserted(mutableListItems.indexOf(bleItemCharacteristic))
            bluetoothGattCharacteristic.descriptors.forEach { bluetoothGattDescriptor ->
                val bleItemDescriptor = BleItem(bluetoothGattDescriptor)
                mutableListItems.add(bleItemDescriptor)
                notifyItemInserted(mutableListItems.indexOf(bleItemDescriptor))
            }
        }
    }

    private fun clear() {
        val size = mutableListItems.size
        mutableListItems.clear()
        notifyItemRangeRemoved(0, size)
    }

    private fun setServices(bluetoothGattServices: List<BluetoothGattService>) {
        clear()
        bluetoothGattServices.forEach { bluetoothGattService ->
            addService(bluetoothGattService)
        }
    }

    fun changeCharacteristicValue(bluetoothGattCharacteristic: BluetoothGattCharacteristic) =
        mutableListItems.indexOfFirst { bleItem ->
            bleItem.type == BleItem.Type.Characteristic
                && bluetoothGattCharacteristic.service.uuid == bleItem.uuidService
                && bluetoothGattCharacteristic.uuid ==
                    bleItem.uuidCharacteristic }.let { position ->
            mutableListItems[position].value = bluetoothGattCharacteristic.value
            notifyItemChanged(position)
        }

    fun changeCharacteristicNotify(bleCharacteristicNotify: BleCharacteristicNotify) {
        mutableListItems.indexOfFirst { bleItem ->
            (bleItem.type == BleItem.Type.Characteristic)
                    && (bleItem.uuidCharacteristic == bleCharacteristicNotify.uuid)
        }.let { position ->
            mutableListItems[position].charNotify = bleCharacteristicNotify.notify
            notifyItemChanged(position)
        }
    }

    fun changeDescriptorValue(bluetoothGattDescriptor: BluetoothGattDescriptor) {
        mutableListItems.indexOfFirst { bleItem ->
            bleItem.type == BleItem.Type.Descriptor
                    && bleItem.uuidService    == bluetoothGattDescriptor.characteristic.service.uuid
                    && bleItem.uuidCharacteristic == bluetoothGattDescriptor.characteristic.uuid
                    && bleItem.uuidDescriptor == bluetoothGattDescriptor.uuid
        }.let { position ->
            if (position >= 0) {
                mutableListItems[position].value = bluetoothGattDescriptor.value
                notifyItemChanged(position)
            }
        }
    }

    fun setOnCharacteristicReadClickListener(listener: (BleItem, View) -> Unit) {
        characteristicReadClickListener = listener
    }

    fun setOnCharacteristicWriteClickListener(listener: (BleItem, View) -> Unit) {
        characteristicWriteClickListener = listener
    }

    fun setOnCharacteristicNotifyClickListener(listener: (BleItem, View) -> Unit) {
        characteristicNotifyClickListener = listener
    }

    fun setOnDescriptorReadClickListener(listener: ((BleItem, View) -> Unit)) {
        descriptorReadClickListener = listener
    }
}