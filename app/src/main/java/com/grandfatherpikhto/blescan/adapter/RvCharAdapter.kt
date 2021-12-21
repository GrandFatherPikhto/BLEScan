package com.grandfatherpikhto.blescan.adapter

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.SendCharValueDialog
import com.grandfatherpikhto.blescan.databinding.BtCharacteristicBinding
import com.grandfatherpikhto.blescan.helper.NAME_UUID
import com.grandfatherpikhto.blescan.helper.isGeneric
import com.grandfatherpikhto.blescan.helper.to16
import com.grandfatherpikhto.blescan.service.*
import com.grandfatherpikhto.blin.BtIoInterface
import com.grandfatherpikhto.blin.BtIoInterfaceLazy
import com.grandfatherpikhto.blin.BtIoListener
import com.grandfatherpikhto.blin.GenericUuids
import kotlinx.coroutines.*
import java.util.*

@InternalCoroutinesApi
@DelicateCoroutinesApi
class RvCharAdapter: RecyclerView.Adapter<RvCharAdapter.CharHolder>() {
    companion object {
        const val TAG:String = "RvCharAdapter"
    }

    private val characteristics: MutableList<Triple<UUID, String, Int>> = mutableListOf()
    private val btIoInterface: BtIoInterface by BtIoInterfaceLazy()
    private val btIoListener = object: BtIoListener {
        override fun onCharacteristicReaded(charUuid: UUID) {
            super.onCharacteristicReaded(charUuid)
            characteristics.indexOfFirst { it.first == charUuid }.let { idx ->
                if(idx >= 0) {
                    btIoInterface.readCharacteristic(charUuid)?.let { value ->
                        if(value.isNotEmpty()) {
                            if(charUuid == NAME_UUID.uuid) {
                                characteristics[idx] = characteristics[idx].copy(second = String(value, charset = Charsets.UTF_8))
                            } else {
                                characteristics[idx] = characteristics[idx].copy(second = value.joinToString { String.format("%02X", it) })
                            }
                            // Log.d(TAG, "onCharacteristicReaded: $charUuid, ${characteristics[idx].second}")
                            // Это ДРУГОЙ поток, поэтому, надо сместить обработку в Handler
                            Handler(Looper.getMainLooper()).post {
                                notifyItemChanged(idx)
                            }
                        }
                    }
                }
            }
        }
    }

    class CharHolder(private val view: View): RecyclerView.ViewHolder(view) {
        private val binding = BtCharacteristicBinding.bind(view)
        private val btIoInterface: BtIoInterface by BtIoInterfaceLazy()
        private val sendCharValueDialog:SendCharValueDialog by lazy {
            SendCharValueDialog()
        }

        fun bind(uuid: UUID, value: String, properties: Int) {
            binding.apply {
                buttonRead.isVisible =
                    properties.and(BluetoothGattCharacteristic.PROPERTY_READ) == BluetoothGattCharacteristic.PROPERTY_READ
                buttonWrite.isVisible =
                    properties.and(BluetoothGattCharacteristic.PROPERTY_WRITE) == BluetoothGattCharacteristic.PROPERTY_WRITE
                buttonNotify.isVisible =
                    properties.and(BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY
                buttonRead.setOnClickListener { _ ->
                    btIoInterface.requestCharacteristic(charUuid = uuid)
                }
                if (uuid.isGeneric()) {
                    tvCharacteristicTitle.text = view.context.getString(R.string.characteristic_title,
                        GenericUuids.genericName(uuid, GenericUuids.Type.Characteristic))
                    tvCharactericticUuid.text  = view.context.getString(R.string.uuid_title,
                        "0x${uuid.to16().toString(16)}")
                } else {
                    tvCharacteristicTitle.text = view.context.getString(R.string.custom_characteristic)
                    tvCharactericticUuid.text  = view.context.getString(R.string.uuid_title,
                        uuid.toString())
                }

                buttonWrite.setOnClickListener { _ ->
                    sendCharValueDialog.uuid = uuid
                    sendCharValueDialog.show((view.context as FragmentActivity).supportFragmentManager, "SendCharValueDialog")
                }

                tvCharacteristicValue.text = value
                Log.d(TAG, "Value: $value")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bt_characteristic, parent, false)
        return RvCharAdapter.CharHolder(view)
    }

    override fun getItemCount(): Int {
        return characteristics.size
    }

    override fun onBindViewHolder(holder: CharHolder, position: Int) {
        Log.d(TAG, "BindViewHolder $position")
        val char = characteristics[position]
        holder.bind(char.first, char.second, char.third)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        btIoInterface.addListener(btIoListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        btIoInterface.removeListener(btIoListener)
    }

    fun setService(service: BluetoothGattService) {
        characteristics.clear()
        service.characteristics.forEach { characteristic ->
            characteristics.add(Triple(characteristic.uuid, "", characteristic.properties))
            notifyItemInserted(characteristics.size - 1)
        }
    }
}