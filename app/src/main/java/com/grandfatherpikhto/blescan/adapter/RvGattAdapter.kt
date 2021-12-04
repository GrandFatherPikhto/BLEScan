package com.grandfatherpikhto.blescan.adapter

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.databinding.BtPropertyBinding
import com.grandfatherpikhto.blescan.model.RvItemClick
import java.util.*

class RvGattAdapter : RecyclerView.Adapter<RvGattAdapter.ProfileHolder>(){
    enum class Type(val value: Int) {
        None(0x00),
        Service(0x01),
        Characteristic(0x02),
        Descriptor(0x03)
    }
    companion object {
        const val TAG:String = "RvGattAdapter"
    }
    /** Список устройств */
    private var profile:MutableList<Pair<Type, UUID>> = mutableListOf()
    private var gatt:BluetoothGatt? = null

    /** Коллбэк для обработки нажатия и долгого нажатия на элемент списка */
    private var itemClick: RvItemClick<Pair<Type, UUID>>? = null
    /** Холдер для лэйаута устройства */
    class ProfileHolder(item: View): RecyclerView.ViewHolder(item) {
        /** Привязка к элементам лэйаута устройства */
        private val binding = BtPropertyBinding.bind(item)
        fun bind(property: Pair<Type, UUID>) {
            with(binding) {
                property.second.also { tvPropertyUuid.text = property.second.toString() }
                when (property.first) {
                    Type.Service -> {
                        binding.tvPropName.text = itemView.context.getString(R.string.service)
                    }
                    Type.Characteristic -> {
                        binding.tvPropName.text = itemView.context.getString(R.string.characteristic)
                    }
                    Type.Descriptor -> {
                        binding.tvPropName.text = itemView.context.getString(R.string.descriptor)
                    }
                    else -> { }
                }

            }
        }
    }

    /** Создаём очередной элемент лэйаута холдера. Операция ресурсоёмкая */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
        Log.d(TAG, "onCreateViewHolder: viewType $viewType")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bt_property, parent, false)
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
            profile.add(Pair( Type.Service, service.uuid))
            service.characteristics?.forEach { characteristic ->
                profile.add(Pair(Type.Characteristic, characteristic.uuid))
                characteristic.descriptors?.forEach { descriptor ->
                    profile.add(Pair(Type.Descriptor, descriptor.uuid))
                }
            }
        }
        notifyDataSetChanged()
    }

    /** Привязка к событию Click */
    fun setOnItemClickListener(itemClick: RvItemClick<Pair<Type, UUID>>) {
        this.itemClick = itemClick
    }
}