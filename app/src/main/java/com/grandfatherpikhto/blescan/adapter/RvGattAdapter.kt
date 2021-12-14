package com.grandfatherpikhto.blescan.adapter

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.databinding.BtCharacteristicBinding
import com.grandfatherpikhto.blescan.databinding.BtDescriptorBinding
import com.grandfatherpikhto.blescan.databinding.BtServiceBinding
import com.grandfatherpikhto.blescan.helper.isGeneric
import com.grandfatherpikhto.blescan.helper.to16
import com.grandfatherpikhto.blescan.model.RvItemClick
import com.grandfatherpikhto.blescan.service.BtCharIO
import com.grandfatherpikhto.blescan.service.GenericUuids
import com.grandfatherpikhto.blescan.service.GenericUuids.genericName
import java.util.*

class RvGattAdapter : RecyclerView.Adapter<RvGattAdapter.ProfileHolder>(){
    enum class ViewType(val value: Int) {
        None(0x00),
        Service(R.layout.bt_service),
        Characteristic(R.layout.bt_characteristic),
        Descriptor(R.layout.bt_descriptor);
        companion object {
            fun getByValue(value: Int):ViewType? = values().firstOrNull { it.value == value }
        }
    }
    companion object {
        const val TAG:String = "RvGattAdapter"
    }
    /** Список устройств */
    private var profile:MutableList<Pair<ViewType, Any>> = mutableListOf()
    private var gatt:BluetoothGatt? = null

    /** Коллбэк для обработки нажатия и долгого нажатия на элемент списка */
    private var itemClick: RvItemClick<Pair<ViewType, Any>>? = null
    /** Холдер для лэйаута устройства */
    class ProfileHolder(private val item: View): RecyclerView.ViewHolder(item) {
        /** Привязка к элементам лэйаута устройства */
        // private val binding = BtPropertyBinding.bind(item)

        private fun bindService(property: Pair<ViewType, Any>) {
            val binding = BtServiceBinding.bind(item)
            val service = property.second as BluetoothGattService
            val uuidParcel = ParcelUuid(service.uuid)
            binding.apply {
                if (uuidParcel.isGeneric()) {
                    tvServiceTitle.text = GenericUuids.genericName(uuidParcel, GenericUuids.Type.Service)
                    tvServiceUuid.text  = "0x${uuidParcel.to16().toString(16)}"
                } else {
                    tvServiceTitle.text = item.context.getString(R.string.custom_service)
                    tvServiceUuid.text  = uuidParcel.toString()
                }
            }
        }

        private fun bindCharacteristic(property: Pair<ViewType, Any>) {
            val binding = BtCharacteristicBinding.bind(item)
            val characteristic = property.second as BluetoothGattCharacteristic
            val uuidParcel = ParcelUuid(characteristic.uuid)
            binding.apply {
                if (uuidParcel.isGeneric()) {
                    tvCharacteristicTitle.text = genericName(uuidParcel, GenericUuids.Type.Characteristic)
                    tvCharactericticUuid.text  = "0x${uuidParcel.to16().toString(16)}"
                } else {
                    tvCharacteristicTitle.text = item.context.getString(R.string.custom_characteristic)
                    tvCharactericticUuid.text  = uuidParcel.toString()
                }
            }
        }

        private fun bindDescriptor(property: Pair<ViewType, Any>) {
            val binding    = BtDescriptorBinding.bind(item)
            val descriptor = property.second as BluetoothGattDescriptor
            val uuidParcel = ParcelUuid(descriptor.uuid)
            binding.apply {
                if (uuidParcel.isGeneric()) {
                    tvDescriptorTitle.text = genericName(uuidParcel, GenericUuids.Type.Descriptor)
                    tvDescriptorUuid.text  = "0x${uuidParcel.to16().toString(16)}"
                } else {
                    tvDescriptorTitle.text = item.context.getString(R.string.custom_descriptor)
                    tvDescriptorUuid.text  = uuidParcel.toString()
                }
            }
        }

        fun bind(property: Pair<ViewType, Any>) {
            when (property.first) {
                ViewType.Service -> {
                    bindService(property)
                }
                ViewType.Characteristic -> {
                    bindCharacteristic(property)
                }
                ViewType.Descriptor -> {
                    bindDescriptor(property)
                }
                else -> { }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        // return super.getItemViewType(position)
        return profile[position].first.value
    }

    /** Создаём очередной элемент лэйаута холдера. Операция ресурсоёмкая */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
        Log.d(TAG, "onCreateViewHolder: viewType $viewType")

        when(ViewType.getByValue(viewType)) {
            ViewType.Service -> {}
            ViewType.Characteristic -> {}
            ViewType.Descriptor -> {}
            else -> {}
        }
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return ProfileHolder(view)
    }

    /** Привязка холдера и события нажатия на элемент к обработчику события */
    override fun onBindViewHolder(holder: ProfileHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemClick?.onItemClick(profile[position], holder.itemView)
        }

        holder.itemView.setOnLongClickListener {
            itemClick?.onItemLongClick(profile[position], holder.itemView)
            return@setOnLongClickListener true
        }

        holder.bind(profile[position])
    }

    /** Вернуть количество элементов списка */
    override fun getItemCount(): Int {
        return profile.size
    }

    /** Очистить список с обновлением отображения */
    fun clearGatt() {
        gatt = null
        profile.clear()
        notifyDataSetChanged()
    }

    /** Залить устройства списком */
    fun setGatt(bluetoothGatt: BluetoothGatt) {
        this.profile.clear()
        gatt = bluetoothGatt
        gatt?.services?.forEach { service ->
            profile.add(Pair(ViewType.Service, service))
            service.characteristics?.forEach { characteristic ->
                profile.add(Pair(ViewType.Characteristic, characteristic))
                characteristic.descriptors?.forEach { descriptor ->
                    profile.add(Pair(ViewType.Descriptor, descriptor))
                }
            }
        }
        notifyDataSetChanged()
    }

    /** Привязка к событию Click */
    fun setOnItemClickListener(itemClick: RvItemClick<Pair<ViewType, Any>>) {
        this.itemClick = itemClick
    }
}