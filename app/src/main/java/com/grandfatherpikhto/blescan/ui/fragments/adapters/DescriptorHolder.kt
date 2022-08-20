package com.grandfatherpikhto.blescan.ui.fragments.adapters

import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.data.BleItem
import com.grandfatherpikhto.blescan.databinding.LayoutDescriptorBinding
import com.grandfatherpikhto.blescan.helper.dpToPx
import com.grandfatherpikhto.blin.GenericUUIDs
import com.grandfatherpikhto.blin.GenericUUIDs.findGeneric
import com.grandfatherpikhto.blin.helper.toHexString
import com.grandfatherpikhto.multistatebutton.MultiStateData
import java.nio.ByteBuffer

class DescriptorHolder (private val view: View) : RecyclerView.ViewHolder(view) {
    private val tagLog = javaClass.simpleName
    private val binding = LayoutDescriptorBinding.bind(view)

    private var descriptorReadClickListener: ((BleItem, View) -> Unit)? = null
    private var descriptorFormatClickListener: ((BleItem, View) -> Unit)? = null

    private var _bleItem:BleItem? = null
    private val bleItem get() = _bleItem!!

    private fun getString(resId: Int, vararg formatArgs: String) =
        view.context.getString(resId, formatArgs)

    private fun showDescriptor() {
        binding.apply {
            if (bleItem.opened) {
                val layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                layoutParams.setMargins(dpToPx(16), dpToPx(8), 0, 0)
                clDescriptor.layoutParams = layoutParams
                clDescriptor.visibility = View.VISIBLE
            } else {
                val layoutParams = clDescriptor.layoutParams
                layoutParams.height = 0
                clDescriptor.layoutParams = layoutParams
                clDescriptor.visibility = View.GONE
            }
        }
    }

    private fun bindDescriptorValue() {
        binding.apply {
            msbDescriptorValueFormat.setStates(RvBleDeviceAdapter.Format.values().map { MultiStateData(it.value) })
            showDescriptorValue()
            msbDescriptorValueFormat.setOnChangeStatusListener { _, _, _, _ ->
                showDescriptorValue()
                descriptorFormatClickListener?.let { listener ->
                    listener(bleItem, view)
                }
            }

            ivDescriptorRead.setOnClickListener { _ ->
                descriptorReadClickListener?.let { listener ->
                    listener(bleItem, view)
                }
            }
        }
    }

    private fun showDescriptorValue() {
        binding.apply {
            RvBleDeviceAdapter.Format.byResId(msbDescriptorValueFormat.state).let { state ->
                if (state.value > 0) {
                tvDescriptorValue.text =
                    when(state) {
                        RvBleDeviceAdapter.Format.Bytes -> {
                            bleItem.value?.toHexString() ?: ""
                        }
                        RvBleDeviceAdapter.Format.Text -> {
                            bleItem.value.toString()
                        }
                        RvBleDeviceAdapter.Format.Integer -> {
                            bleItem.value?.let {
                                if (it.size == Int.SIZE_BYTES) {
                                    ByteBuffer.wrap(it).int
                                } else ""
                            }
                            ""
                        }
                        RvBleDeviceAdapter.Format.Float -> {
                            bleItem.value?.let {
                                if (it.size == Float.SIZE_BYTES) {
                                    ByteBuffer.wrap(it).float
                                } else ""
                            }
                            ""
                        }
                    }
                }
            }
        }
    }

    fun bind(item: BleItem) {
        _bleItem = item
        binding.apply {
            tvDescriptorUuid.text = bleItem.uuidDescriptor.toString().uppercase()
            tvDescriptorName.text = bleItem.uuidDescriptor
                ?.findGeneric(GenericUUIDs.Type.Descriptor)?.name
                    ?: getString(R.string.custom_descriptor)
        }

        showDescriptor()
        bindDescriptorValue()
    }

    fun setOnDescriptorReadClickListener(listener: (BleItem, View) -> Unit) {
        descriptorReadClickListener = listener
    }

    fun setOnDescriptorValueFormatClickListener(listener: (BleItem, View) -> Unit) {
        descriptorFormatClickListener = listener
    }
}