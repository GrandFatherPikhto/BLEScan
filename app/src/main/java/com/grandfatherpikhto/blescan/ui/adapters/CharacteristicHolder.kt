package com.grandfatherpikhto.blescan.ui.adapters

import android.bluetooth.BluetoothGattCharacteristic
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blin.GenericUUIDs
import com.grandfatherpikhto.blin.GenericUUIDs.genericName
import com.grandfatherpikhto.blin.GenericUUIDs.genericStringUUID
import com.grandfatherpikhto.blin.helper.hasFlag
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.data.CharacteristicData
import com.grandfatherpikhto.blescan.databinding.LayoutCharacteristicBinding
import com.grandfatherpikhto.blescan.helper.dpToPx
import com.grandfatherpikhto.multistatebutton.MultiStateData
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CharacteristicHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private val binding = LayoutCharacteristicBinding.bind(view)

    private fun getString(resId: Int, vararg formatArgs: String) = view.context.getString(resId, formatArgs)

    private var characteristicReadClickListener: ((CharacteristicData, View) -> Unit)? = null
    private var characteristicNotifyClickListener: ((CharacteristicData, View) -> Unit)? = null
    private var characteristicWriteClickListener: ((CharacteristicData, View) -> Unit)? = null
    private var characteristicFormatClickListener: ((CharacteristicData, View) -> Unit)? = null

    private val characteristicProperties = listOf(
        CharacteristicProperty(
            BluetoothGattCharacteristic.PROPERTY_BROADCAST,
            R.string.characteristic_property_broadcast),
        CharacteristicProperty(
            BluetoothGattCharacteristic.PROPERTY_READ,
            R.string.characteristic_property_read),
        CharacteristicProperty(
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            R.string.characteristic_property_write_no_response),
        CharacteristicProperty(
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            R.string.characteristic_property_write),
        CharacteristicProperty(
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            R.string.characteristic_property_notify),
        CharacteristicProperty(
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            R.string.characteristic_property_indicate),
        CharacteristicProperty(
            BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
            R.string.characteristic_property_signed_write),
        CharacteristicProperty(
            BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS,
            R.string.characteristic_property_extended_props)
    )

    private fun textCharacteristicProperties(bluetoothGattCharacteristic: BluetoothGattCharacteristic) : String {
        val propertiesList = mutableListOf<String>()
        characteristicProperties.forEach { characteristicProperty ->
            if (bluetoothGattCharacteristic.properties.hasFlag(characteristicProperty.property)) {
                propertiesList.add(getString(characteristicProperty.resId))
            }
        }

        return propertiesList.joinToString(", ")
    }

    private fun showPropertyIcons(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        binding.apply {
            if (bluetoothGattCharacteristic
                    .properties.hasFlag(BluetoothGattCharacteristic.PROPERTY_READ)
            ) {
                ibRead.visibility = View.VISIBLE
//                ibFormat.visibility = View.VISIBLE
            } else {
                ibRead.visibility = View.GONE
//                ibFormat.visibility = View.GONE
            }

            if (bluetoothGattCharacteristic
                    .properties.hasFlag(BluetoothGattCharacteristic.PROPERTY_WRITE)
            ) {
                ibWrite.visibility = View.VISIBLE
            } else {
                ibWrite.visibility = View.GONE
            }

            if (bluetoothGattCharacteristic
                    .properties.hasFlag(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
            ) {
                ibNotify.setImageResource(R.drawable.ic_notify_on)
            } else {
                ibNotify.setImageResource(R.drawable.ic_notify_off)
            }
        }
    }

    private fun bindCharacteristicFormatIcon(characteristicData: CharacteristicData) {
        binding.apply {
            msbFormat.setStates(RvBleDeviceAdapter.Format.values().map { MultiStateData(it.value) })
            showCharacteristicValue(characteristicData)
            msbFormat.setOnChangeStatusListener { _, resId, _, msbView ->
                RvBleDeviceAdapter.Format.byResId(resId)?.let { format ->
                    characteristicData.format = format
                    showCharacteristicValue(characteristicData)
                    characteristicFormatClickListener?.let { listener ->
                        listener(characteristicData, msbView)
                    }
                }
            }
        }
    }

    private fun bindClickCharacteristicIcons(characteristicData: CharacteristicData
    ) {
        binding.apply {
            ibRead.setOnClickListener { ivView ->
                characteristicReadClickListener?.let { listener ->
                    listener(characteristicData, ivView)
                }
            }

            ibWrite.setOnClickListener { ivView ->
                characteristicWriteClickListener?.let { listener ->
                    listener(characteristicData, ivView)
                }
            }

            ibNotify.setOnClickListener { ivView ->
                characteristicNotifyClickListener?.let { listener ->
                    listener(characteristicData, ivView)
                }
            }
        }
    }

    private fun showCharacteristicValue(characteristicData: CharacteristicData) {
        binding.apply {
            characteristicData.bluetoothGattCharacteristic.value?.let { value ->
                RvBleDeviceAdapter.Format.byResId(binding.msbFormat.state)?.let { state ->
                    msbFormat.enableState(RvBleDeviceAdapter.Format.Integer.value,
                        value.size == Int.SIZE_BYTES)
                    msbFormat.enableState(RvBleDeviceAdapter.Format.Float.value,
                            value.size == Float.SIZE_BYTES)
                    tvCharacteristicValue.text =
                        when(state) {
                            RvBleDeviceAdapter.Format.Integer -> {
                                if (value.size == Int.SIZE_BYTES) {
                                    String.format(
                                        "%04d",
                                        ByteBuffer.wrap(value)
                                            .order(ByteOrder.BIG_ENDIAN).int
                                    )
                                } else {
                                    ""
                                }
                            }
                            RvBleDeviceAdapter.Format.Bytes -> {
                                value.joinToString (", "){ String.format("%02X", it) }
                            }
                            RvBleDeviceAdapter.Format.Text -> {
                                String(value)
                            }
                            RvBleDeviceAdapter.Format.Float -> {
                                if (value.size == Float.SIZE_BYTES) {
                                    ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN).float.toString()
                                } else ""
                            }
                        }
                }
            }
        }
    }

    private fun showCharacteristic(visible: Boolean) {
        binding.apply {
            if (visible) {
                val layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                layoutParams.setMargins(dpToPx(8), dpToPx(8),0, 0)
                clCharacteristicLayout.layoutParams = layoutParams
                clCharacteristicLayout.visibility = View.VISIBLE
            } else {
                val layoutParams = ConstraintLayout.LayoutParams(0, 0)
                layoutParams.height = 0
                layoutParams.setMargins(0)
                clCharacteristicLayout.layoutParams = layoutParams
                clCharacteristicLayout.visibility = View.GONE
            }
        }
    }


    fun bind(characteristicData: CharacteristicData) {
        binding.apply {
            tvCharacteristicName.text = characteristicData.bluetoothGattCharacteristic.uuid
                .genericName(getString(R.string.custom_characteristic))
            tvCharacteristicUuid.text = characteristicData.bluetoothGattCharacteristic.uuid
                .genericStringUUID(type = GenericUUIDs.Type.Characteristic)
            tvCharacteristicProperties.text =
                textCharacteristicProperties(characteristicData.bluetoothGattCharacteristic)
        }

        showCharacteristicValue(characteristicData)
        showPropertyIcons(characteristicData.bluetoothGattCharacteristic)
        showCharacteristic(characteristicData.visible)
        bindClickCharacteristicIcons(characteristicData)
        bindCharacteristicFormatIcon(characteristicData)
    }

    fun setOnCharacteristicReadClickListener(listener: (CharacteristicData, View) -> Unit) {
        characteristicReadClickListener = listener
    }

    fun setOnCharacteristicWriteClickListener(listener: (CharacteristicData, View) -> Unit) {
        characteristicWriteClickListener = listener
    }

    fun setOnCharacteristicNotifyClickListener(listener: (CharacteristicData, View) -> Unit) {
        characteristicNotifyClickListener = listener
    }

    fun setOnCharacteristicFormatClickListener(listener: (CharacteristicData, View) -> Unit) {
        characteristicFormatClickListener = listener
    }
}