package com.grandfatherpikhto.blescan.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.databinding.BtDeviceBinding
import com.grandfatherpikhto.blescan.model.BtLeDevice
import com.grandfatherpikhto.blescan.model.RvItemClick

class RvBtAdapter : RecyclerView.Adapter<RvBtAdapter.RvBtDeviceHolder>(){
    /** Список устройств */
    private val leDevices:MutableList<BtLeDevice> = mutableListOf()

    /** Коллбэк для обработки нажатия и долгого нажатия на элемент списка */
    private var itemClick: RvItemClick<BtLeDevice>? = null
    /** Холдер для лэйаута устройства */
    class RvBtDeviceHolder(item: View): RecyclerView.ViewHolder(item) {
        /** Привязка к элементам лэйаута устройства */
        private val binding = BtDeviceBinding.bind(item)

        fun bind(btLeDevice: BtLeDevice) {
            with(binding) {
                btLeDevice.name.also {
                    if(it != null) {
                        if (it.isEmpty()) {
                            tvBtDeviceName.text =
                                itemView.context.getString(R.string.default_bt_name)
                        } else {
                            tvBtDeviceName.text = it
                        }
                    }
                }
                btLeDevice.address.also { tvBtDeviceAddress.text = btLeDevice.address }
                if(btLeDevice.bondState == BluetoothDevice.BOND_NONE) {
                    ivBtPaired.setImageResource(R.drawable.ic_baseline_bluetooth_24)
                } else {
                    ivBtPaired.setImageResource(R.drawable.ic_baseline_bluetooth_connected_24)
                }
            }
        }
    }

    /** Создаём очередной элемент лэйаута холдера. Операция ресурсоёмкая */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvBtDeviceHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bt_device, parent, false)
        return RvBtDeviceHolder(view)
    }

    /** Привязка холдера и события нажатия на элемент к обработчику события */
    override fun onBindViewHolder(holder: RvBtDeviceHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemClick?.onItemClick(leDevices[position], holder.itemView)
        }

        holder.itemView.setOnLongClickListener {
            itemClick?.onItemLongClick(leDevices[position], holder.itemView)
            return@setOnLongClickListener true
        }

        holder.bind(leDevices[position])
    }

    /** Вернуть количество элементов списка */
    override fun getItemCount(): Int {
        return leDevices.size
    }

    /** Добавить устройство в список с обновлением списка */
    fun addBtDevice(leDevice: BtLeDevice) {
        if(!leDevices.contains(leDevice)) {
            val exist: BtLeDevice? = leDevices.find { it.address.equals(leDevice.address) }
            if (exist == null) {
                leDevices.add(leDevice)
                notifyDataSetChanged()
            }
        }
    }

    /** Очистить список с обновлением отображения */
    fun clearBtDevices() {
        leDevices.clear()
        notifyDataSetChanged()
    }

    /** Залить устройства списком */
    fun setBtDevicesList(leDevices: List<BtLeDevice>) {
        this.leDevices.clear()
        this.leDevices.addAll(leDevices)
        notifyDataSetChanged()

    }

    /** Это для работы со списком устройств, который возвращает  */
    fun setBtDevices(leDevices: Set<BtLeDevice>) {
        this.leDevices.clear()
        this.leDevices.addAll(leDevices)
        notifyDataSetChanged()
    }


    /** Привязка к событию Click */
    fun setOnItemClickListener(itemClick: RvItemClick<BtLeDevice>) {
        this.itemClick = itemClick
    }
}