package com.grandfatherpikhto.blescan.adapter

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.databinding.BtCharacteristicBinding
import com.grandfatherpikhto.blescan.helper.isGeneric
import com.grandfatherpikhto.blescan.helper.to16
import com.grandfatherpikhto.blescan.service.BluetoothInterface
import com.grandfatherpikhto.blescan.service.BluetoothInterfaceLazy
import com.grandfatherpikhto.blescan.service.BluetoothListener
import com.grandfatherpikhto.blescan.service.GenericUuids
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect

@InternalCoroutinesApi
@DelicateCoroutinesApi
class RvCharAdapter: RecyclerView.Adapter<RvCharAdapter.CharHolder>() {
    companion object {
        const val TAG:String = "RvCharAdapter"
    }

    private val characteristics: MutableList<Pair<BluetoothGattCharacteristic, String>> = mutableListOf()
    private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()
    private var bluetoothService: BluetoothGattService? = null
    private val bluetoothListener = object : BluetoothListener {
        override fun onCharacteristicReaded(
            bluetoothGatt: BluetoothGatt?,
            bluetoothGattCharacteristic: BluetoothGattCharacteristic?,
            state: Int
        ) {
            super.onCharacteristicReaded(bluetoothGatt, bluetoothGattCharacteristic, state)
            bluetoothGattCharacteristic?.let { char ->
                characteristics.indexOfFirst { char.uuid == it.first.uuid }.let { idx ->
                    if(idx >= 0) {
                        Log.d(TAG, "onCharacteristicReaded $idx, ${bluetoothGattCharacteristic?.uuid}, ${char.uuid}, ${bluetoothGattCharacteristic?.value.toString()}")
                        // Сервис имени. Получаем строку
                        if(char.uuid == ParcelUuid.fromString("00002a00-0000-1000-8000-00805f9b34fb").uuid) {
                            characteristics[idx] = characteristics[idx].copy(second = String(char.value, charset = Charsets.UTF_8))
                        } else {
                            characteristics[idx] = characteristics[idx].copy(second = char.value.joinToString { String.format("%02X", it) })
                        }
                        /**
                         * Это ДРУГОЙ поток, поэтому, надо сместить обработку в Handler
                         */
                        Handler(Looper.getMainLooper()).post {
                            notifyItemChanged(idx)
                        }
                    }
                }
            }
        }
    }


    class CharHolder(private val view: View): RecyclerView.ViewHolder(view) {
        private val binding = BtCharacteristicBinding.bind(view)
        private val bluetoothInterface: BluetoothInterface by BluetoothInterfaceLazy()

        fun bind(char: Pair<BluetoothGattCharacteristic, String>) {
            binding.apply {
                buttonRead.isVisible =
                    char.first.properties.and(BluetoothGattCharacteristic.PROPERTY_READ) == BluetoothGattCharacteristic.PROPERTY_READ
                buttonWrite.isVisible =
                    char.first.properties.and(BluetoothGattCharacteristic.PROPERTY_WRITE) == BluetoothGattCharacteristic.PROPERTY_WRITE
                buttonNotify.isVisible =
                    char.first.properties.and(BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY
                buttonRead.setOnClickListener { _ ->
                    bluetoothInterface.readCharacteristic(char.first)
                }
                if (char.first.uuid.isGeneric()) {
                    tvCharacteristicTitle.text = view.context.getString(R.string.characteristic_title,
                        GenericUuids.genericName(char.first.uuid, GenericUuids.Type.Characteristic))
                    tvCharactericticUuid.text  = view.context.getString(R.string.uuid_title,
                        "0x${char.first.uuid.to16().toString(16)}")
                } else {
                    tvCharacteristicTitle.text = view.context.getString(R.string.custom_characteristic)
                    tvCharactericticUuid.text  = view.context.getString(R.string.uuid_title,
                        char.first.uuid.toString())
                }

                tvCharacteristicValue.text = char.second
                Log.d(TAG, "Value: ${char.second}")
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
        holder.bind(characteristics[position])
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        bluetoothInterface.addListener(bluetoothListener)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        bluetoothInterface.removeListener(bluetoothListener)
    }

    fun setService(service: BluetoothGattService) {
        characteristics.clear()
        bluetoothService = service
        service.characteristics.forEach { characteristic ->
            characteristics.add(Pair<BluetoothGattCharacteristic, String>(characteristic, "TEST"))
            notifyItemInserted(characteristics.size - 1)
        }
    }
}