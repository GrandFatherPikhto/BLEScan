package com.grandfatherpikhto.blescan.ui.fragments.adapters

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blin.data.BleGatt
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.data.CharacteristicData
import com.grandfatherpikhto.blescan.data.DescriptorData
import com.grandfatherpikhto.blescan.data.ServiceData
import com.grandfatherpikhto.blin.buffer.BleCharacteristicNotify
import kotlin.properties.Delegates

class RvBleDeviceAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder> () {
    private val mutableListItems = mutableListOf<Any>()
    private val tagLog = this.javaClass.simpleName

    private var characteristicReadClickListener: ((BluetoothGattCharacteristic, View) -> Unit)? = null
    private var characteristicNotifyClickListener: ((BluetoothGattCharacteristic, View) -> Unit)? = null
    private var characteristicWriteClickListener: ((BluetoothGattCharacteristic, View) -> Unit)? = null
    private var characteristicFormatClickListener: ((BluetoothGattCharacteristic, Format, View) -> Unit)? = null

    enum class Format(val value: Int) {
        Bytes(R.drawable.ic_bytes),
        Text(R.drawable.ic_text),
        Integer(R.drawable.ic_integer),
        Float(R.drawable.ic_float);

        companion object {
            fun byResId(value: Int) = values().find { value == it.value }
        }
    }

    var bleGatt:BleGatt? by Delegates.observable(null) { _, _, value ->
        if (value == null) {
            clear()
        } else {
            setServices(value.services)
        }
    }

    enum class Type (val value:Int) {
        None(0x00),
        Service(0x01),
        Characteristic(0x02),
        Descriptor(0x03)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            Type.Service.value -> {
                ServiceHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_service, parent, false))
            }
            Type.Characteristic.value -> {
                CharacteristicHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_characteristic, parent, false))
            }
            Type.Descriptor.value -> {
                DescriptorHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_descriptor, parent, false))
            }
            else -> { throw IllegalArgumentException("There is no such type $viewType") }
        }
    }

    private fun updateNestedItems(serviceData: ServiceData) {
        mutableListItems.filterIndexed { index, any ->
            when (any) {
                is CharacteristicData -> {
                    if (any.bluetoothGattCharacteristic.service.uuid
                        == serviceData.bluetoothGattService.uuid) {
                        (mutableListItems[index] as CharacteristicData).visible = serviceData.opened
                        notifyItemChanged(index)
                        true
                    } else false
                }
                is DescriptorData -> {
                    if (any.bluetoothGattDescriptor.characteristic.service.uuid
                        == serviceData.bluetoothGattService.uuid) {
                        (mutableListItems[index] as DescriptorData).visible = serviceData.opened
                        notifyItemChanged(index)
                        true
                    } else false
                }
                is ServiceData -> {
                    false
                }
                else -> {
                    Log.d(tagLog, "Неизвестный тип")
                    false }
            }
        }
    }

    private fun bindCharacteristicHolderListeners(holder: CharacteristicHolder) {
        holder.setOnCharacteristicReadClickListener { characteristicData, view ->
            characteristicReadClickListener?.let { listener ->
                listener(characteristicData.bluetoothGattCharacteristic, view)
            }
        }
        holder.setOnCharacteristicNotifyClickListener { characteristicData, view ->
            characteristicNotifyClickListener?.let { listener ->
                listener(characteristicData.bluetoothGattCharacteristic, view)
            }
        }
        holder.setOnCharacteristicWriteClickListener { characteristicData, view ->
            characteristicWriteClickListener?.let { listener ->
                listener(characteristicData.bluetoothGattCharacteristic, view)
            }
        }
    }

    private fun bindCharacteristicHolder(holder: CharacteristicHolder, position: Int) {
        holder.bind(mutableListItems[position] as CharacteristicData)
        bindCharacteristicHolderListeners(holder)
        holder.setOnCharacteristicFormatClickListener { characteristicData, format, view ->
            val index = mutableListItems.indexOf(characteristicData)
            characteristicFormatClickListener?.let { listener ->
                listener(characteristicData.bluetoothGattCharacteristic, format, view)
            }
        }
    }

    private fun bindServiceHolder(holder: ServiceHolder, position: Int) {
        holder.bind(mutableListItems[position] as ServiceData)
        holder.itemView.setOnClickListener { _ ->
            val serviceData = mutableListItems[position] as ServiceData
            (mutableListItems[position] as ServiceData).opened = !serviceData.opened
            notifyItemChanged(mutableListItems.indexOf(serviceData))
            updateNestedItems(mutableListItems[position] as ServiceData)
        }
    }

    private fun bindDescriptorHolder(holder: DescriptorHolder, position: Int) {
        holder.bind(mutableListItems[position] as DescriptorData)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Чтобы определять тип, надо работать сразу с полученным значением
        // Без переприсваивания bleItem!!
        when (holder.itemViewType) {
            Type.Service.value -> {
                bindServiceHolder(holder as ServiceHolder, position)
            }
            Type.Characteristic.value -> {
                bindCharacteristicHolder(holder as CharacteristicHolder, position)
            }
            Type.Descriptor.value -> {
                bindDescriptorHolder(holder as DescriptorHolder, position)
            }
            else -> {
                Log.d(tagLog, "Не пойми, что")
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        // return super.getItemViewType(position)
        return when (mutableListItems[position]) {
            is ServiceData -> Type.Service.value
            is CharacteristicData -> Type.Characteristic.value
            is DescriptorData -> Type.Descriptor.value
            else -> Type.None.value
        }
    }

    override fun getItemCount(): Int = mutableListItems.size

    fun addService(bluetoothGattService: BluetoothGattService) {
        var size = 1
        mutableListItems.add(ServiceData(bluetoothGattService))
        bluetoothGattService.characteristics.forEach { bluetoothGattCharacteristic ->
            mutableListItems.add(CharacteristicData(bluetoothGattCharacteristic))
            size ++
            bluetoothGattCharacteristic.descriptors.forEach { bluetoothGattDescriptor ->
                mutableListItems.add(DescriptorData(bluetoothGattDescriptor))
                size ++
            }
        }
        notifyItemRangeInserted(mutableListItems.indexOf(bluetoothGattService), size)
    }

    fun clear() {
        val size = mutableListItems.size
        mutableListItems.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun setServices(bluetoothGattServices: List<BluetoothGattService>) {
        clear()
        bluetoothGattServices.forEach { bluetoothGattService ->
            addService(bluetoothGattService)
        }
    }

    fun changeCharacteristicValue(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        val index =
            mutableListItems.indexOfFirst { it is CharacteristicData
                    && it.bluetoothGattCharacteristic.uuid == bluetoothGattCharacteristic.uuid }
        if (index >= 0) {
            mutableListItems[index] = CharacteristicData(bluetoothGattCharacteristic,
                visible = (mutableListItems[index] as CharacteristicData).visible)
            notifyItemChanged(index)
        }
    }

    fun changeDescriptor(bluetoothGattDescriptor: BluetoothGattDescriptor) {
        val idx =
            mutableListItems.indexOfFirst { it is DescriptorData
                    && it.bluetoothGattDescriptor.uuid == bluetoothGattDescriptor.uuid }
        if (idx >= 0) {
            mutableListItems[idx] = bluetoothGattDescriptor
        }
    }

    fun changeCharacteristicNotify(bleCharacteristicNotify: BleCharacteristicNotify) {
        mutableListItems.forEachIndexed { index, any ->
            if (any is CharacteristicData
                && any.bluetoothGattCharacteristic.uuid
                == bleCharacteristicNotify.bluetoothGattCharacteristic.uuid) {
                (mutableListItems[index] as CharacteristicData).notify = bleCharacteristicNotify.notify
                notifyItemChanged(index)
            }
        }
    }

    fun setOnCharacteristicReadClickListener(listener: (BluetoothGattCharacteristic, View) -> Unit) {
        characteristicReadClickListener = listener
    }

    fun setOnCharacteristicWriteClickListener(listener: (BluetoothGattCharacteristic, View) -> Unit) {
        characteristicWriteClickListener = listener
    }

    fun setOnCharacteristicNotifyClickListener(listener: (BluetoothGattCharacteristic, View) -> Unit) {
        characteristicNotifyClickListener = listener
    }

    fun setOnCharacteristicFormatClickListener(listener: (BluetoothGattCharacteristic, Format, View) -> Unit) {
        characteristicFormatClickListener = listener
    }
}