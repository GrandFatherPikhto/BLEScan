package com.grandfatherpikhto.blescan.ui.fragments.adapters

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.BleScanApp
import com.grandfatherpikhto.blin.GenericUUIDs
import com.grandfatherpikhto.blin.GenericUUIDs.genericStringUUID
import com.grandfatherpikhto.blin.helper.hasFlag
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.data.CharacteristicData
import com.grandfatherpikhto.blescan.databinding.LayoutCharacteristicBinding
import com.grandfatherpikhto.blescan.helper.dpToPx
import com.grandfatherpikhto.blin.BleManager
import com.grandfatherpikhto.blin.BleManagerInterface
import com.grandfatherpikhto.blin.GenericUUIDs.findGeneric
import com.grandfatherpikhto.multistatebutton.MultiStateData
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CharacteristicHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private val tagLog = javaClass.simpleName

    private val binding = LayoutCharacteristicBinding.bind(view)

    private fun getString(resId: Int, vararg formatArgs: String) = view.context.getString(resId, formatArgs)

    private var characteristicData: CharacteristicData? = null

    private var characteristicReadClickListener: ((CharacteristicData, View) -> Unit)? = null
    private var characteristicNotifyClickListener: ((CharacteristicData, View) -> Unit)? = null
    private var characteristicWriteClickListener: ((CharacteristicData, View) -> Unit)? = null
    private var characteristicFormatClickListener: ((CharacteristicData, RvBleDeviceAdapter.Format, View) -> Unit)? = null

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

    private fun writeType() {
        characteristicData?.let { data ->
            data.bluetoothGattCharacteristic.writeType.let { writeType ->
                if (writeType.hasFlag(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)) {
                    Log.d("Write Type", "Default")
                }

                if (writeType.hasFlag(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)) {
                    Log.d("Write Type", "No Response")
                }

                if (writeType.hasFlag(BluetoothGattCharacteristic.WRITE_TYPE_SIGNED)) {
                    Log.d("Write Type", "Signed")
                }
            }
        }
    }

    private fun textCharacteristicProperties() : String {
        val propertiesList = mutableListOf<String>()
        characteristicData?.let { data ->
            characteristicProperties.forEach { characteristicProperty ->
                if (data.bluetoothGattCharacteristic
                        .properties.hasFlag(characteristicProperty.property)) {
                    propertiesList.add(getString(characteristicProperty.resId))
                }
            }
        }

        return propertiesList.joinToString(", ")
    }

    private fun showPropertyIcons() {
        characteristicData?.let { data ->
            data.bluetoothGattCharacteristic.properties.let { properties ->
                binding.apply {
                    if (properties.hasFlag(BluetoothGattCharacteristic.PROPERTY_READ)
                    ) {
                        ibRead.visibility = View.VISIBLE
                    } else {
                        ibRead.visibility = View.GONE
                    }

                    if (properties.hasFlag(BluetoothGattCharacteristic.PROPERTY_WRITE)
                    ) {
                        ibWrite.visibility = View.VISIBLE
                    } else {
                        ibWrite.visibility = View.GONE
                    }

                    if (properties.hasFlag(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
                    ) {
                        ibWrite.visibility = View.VISIBLE
                        if (data.notify) {
                            ibNotify.setImageResource(R.drawable.ic_notify_on)
                        } else {
                            ibNotify.setImageResource(R.drawable.ic_notify_off)
                        }
                    } else {
                        ibWrite.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun bindCharacteristicFormatIcon() {
        characteristicData?.let { data ->
            binding.apply {
                msbFormat.setStates(RvBleDeviceAdapter.Format.values().map { MultiStateData(it.value) })
                showCharacteristicValue()
                msbFormat.setOnChangeStatusListener { _, resId, _, msbView ->
                    RvBleDeviceAdapter.Format.byResId(resId)?.let { format ->
                        showCharacteristicValue()
                        characteristicFormatClickListener?.let { listener ->
                            listener(data, format, msbView)
                        }
                    }
                }
            }
        }
    }

    private fun bindNotifyCharacteristicIcon() = characteristicData?.let { data ->
        characteristicData?.let { data ->
            binding.apply {
                ibNotify.setOnClickListener { _ ->
                    characteristicNotifyClickListener?.let { listener ->
                        listener(data, view)
                    }
                }
            }
        }
    }


    private fun bindClickCharacteristicIcons() {
        characteristicData?.let { data ->
            binding.apply {
                ibRead.setOnClickListener { ivView ->
                    characteristicReadClickListener?.let { listener ->
                        listener(data, ivView)
                    }
                }

                ibWrite.setOnClickListener { ivView ->
                    characteristicWriteClickListener?.let { listener ->
                        listener(data, ivView)
                    }
                }

                ibNotify.setOnClickListener { ivView ->
                    characteristicNotifyClickListener?.let { listener ->
                        listener(data, ivView)
                    }
                }
            }
        }
    }

    private fun showCharacteristicValue() {
        characteristicData?.let { data ->
            binding.apply {
                data.bluetoothGattCharacteristic.value?.let { value ->
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
    }

    private fun showCharacteristic() {
        characteristicData?.let { charData ->
            binding.apply {
                if (charData.visible) {
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
    }

    private fun genericValueFormat() {
        characteristicData?.let { data ->
            binding.apply {
                data.bluetoothGattCharacteristic.uuid.findGeneric()?.let { uuiD16 ->
                    if (uuiD16.uuid == 0x2A00) {
                        msbFormat.enableState(RvBleDeviceAdapter.Format.Integer.value, false)
                        msbFormat.enableState(RvBleDeviceAdapter.Format.Float.value, false)
                        msbFormat.enableState(RvBleDeviceAdapter.Format.Bytes.value, false)
                        msbFormat.setCurrentResId(RvBleDeviceAdapter.Format.Text.value)
                        Log.d(tagLog, "Generic Name")
                    }
                }
            }
        }
    }

    fun bind(data: CharacteristicData) {
        this.characteristicData = data
        binding.apply {
            tvCharacteristicName.text = data.bluetoothGattCharacteristic.uuid
                .findGeneric()?.name ?: getString(R.string.custom_characteristic)
            tvCharacteristicUuid.text = data.bluetoothGattCharacteristic.uuid
                .genericStringUUID(type = GenericUUIDs.Type.Characteristic)
            tvCharacteristicProperties.text =
                textCharacteristicProperties()
        }
        writeType()
        bindClickCharacteristicIcons()
        bindCharacteristicFormatIcon()
        bindNotifyCharacteristicIcon()
        showCharacteristicValue()
        genericValueFormat()
        showPropertyIcons()
        showCharacteristic()
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

    fun setOnCharacteristicFormatClickListener(listener: (CharacteristicData, RvBleDeviceAdapter.Format, View) -> Unit) {
        characteristicFormatClickListener = listener
    }
}