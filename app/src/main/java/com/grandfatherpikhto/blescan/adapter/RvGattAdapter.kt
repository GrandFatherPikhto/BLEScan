package com.grandfatherpikhto.blescan.adapter

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import android.os.ParcelUuid
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.databinding.BtServiceBinding
import com.grandfatherpikhto.blescan.helper.isGeneric
import com.grandfatherpikhto.blescan.helper.to16
import com.grandfatherpikhto.blescan.model.RvItemClick
import com.grandfatherpikhto.blin.listeners.BluetoothInterface
import com.grandfatherpikhto.blin.listeners.loaders.BluetoothInterfaceLazy
import com.grandfatherpikhto.blin.helper.GenericUuids
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
class RvGattAdapter : RecyclerView.Adapter<RvGattAdapter.ProfileHolder>() {
    companion object {
        const val TAG:String = "RvGattAdapter"
    }
    /** Список устройств */
    private var services:MutableList<BluetoothGattService> = mutableListOf()
    private var gatt:BluetoothGatt? = null

    /** Коллбэк для обработки нажатия и долгого нажатия на элемент списка */
    private var itemClick: RvItemClick<BluetoothGattService>? = null
    /** Холдер для лэйаута устройства */
    class ProfileHolder(private val item: View): RecyclerView.ViewHolder(item) {
        val binding = BtServiceBinding.bind(item)
        private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()
        private val rvCharAdapter: RvCharAdapter by lazy {
            RvCharAdapter()
        }

        /**
         * Привязка плашек (layouts) к данным
         */
        fun bind(service: BluetoothGattService) {
            val uuidParcel = ParcelUuid(service.uuid)
            binding.apply {
                if (uuidParcel.isGeneric()) {
                    tvServiceTitle.text = item.context.getString(R.string.service_title
                        , GenericUuids.genericName(uuidParcel, GenericUuids.Type.Service))
                    tvServiceUuid.text = item.context.getString(R.string.uuid_title, "0x${uuidParcel.to16().toString(16)}")
                } else {
                    tvServiceTitle.text = item.context.getString(R.string.custom_service)
                    tvServiceUuid.text  = item.context.getString(R.string.uuid_title, uuidParcel.toString())
                }
                rvChars.adapter = rvCharAdapter
                rvCharAdapter.setService(service)
                rvChars.layoutManager = LinearLayoutManager(item.context)
            }
        }
    }

    /**
     * Создаём очередной элемент лэйаута холдера. Операция ресурсоёмкая
     * Поскольку enum Type содержит id плашки, создание происходит очень
     * просто
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bt_service, parent,false)
        return ProfileHolder(view)
    }

    /** Привязка холдера и события нажатия на элемент к обработчику события */
    override fun onBindViewHolder(holder: ProfileHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemClick?.onItemClick(services[position], holder.itemView)
        }

        holder.itemView.setOnLongClickListener {
            itemClick?.onItemLongClick(services[position], holder.itemView)
            return@setOnLongClickListener true
        }

        holder.bind(services[position])
    }

    /** Вернуть количество элементов списка */
    override fun getItemCount(): Int {
        return services.size
    }

    /** Очистить список с обновлением отображения */
    fun clearGatt() {
        gatt = null
        services.clear()
        notifyDataSetChanged()
    }

    /** Залить устройства списком */
    fun setGatt(bluetoothGatt: BluetoothGatt) {
        services.clear()
        gatt = bluetoothGatt
        gatt?.services?.forEach { service ->
            services.add(service)
        }
        notifyDataSetChanged()
    }

    /** Привязка к событию Click */
    fun setOnItemClickListener(itemClick: RvItemClick<BluetoothGattService>) {
        this.itemClick = itemClick
    }
}