package com.grandfatherpikhto.blescan.ui.fragments.adapters

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.data.DescriptorData
import com.grandfatherpikhto.blescan.databinding.LayoutDescriptorBinding
import com.grandfatherpikhto.blescan.helper.dpToPx

class DescriptorHolder (view: View) : RecyclerView.ViewHolder(view) {

    private val binding = LayoutDescriptorBinding.bind(view)

    private var descriptorReadClickListener: ((DescriptorData, View) -> Unit)? = null
    private var descriptorFormatClickListener: ((DescriptorData, View) -> Unit)? = null


    fun bind(descriptorData: DescriptorData) {
        binding.apply {
            tvDescriptorUuid.text = descriptorData.bluetoothGattDescriptor.uuid.toString().uppercase()

        }

        showDescriptor(binding, descriptorData.visible)
    }


    private fun showDescriptor(binding: LayoutDescriptorBinding, visible: Boolean) {
        binding.apply {
            if (visible) {
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

    fun setOnDescriptorReadClickListener(listener: (DescriptorData, View) -> Unit) {
        descriptorReadClickListener = listener
    }

    fun setOnDescriptorValueFormatClickListener(listener: (DescriptorData, View) -> Unit) {
        descriptorFormatClickListener = listener
    }
    private fun descriptorValue(descriptorData: DescriptorData) : String? {

        return null
    }


    fun bindDescriptorIcons(binding: LayoutDescriptorBinding, descriptorData: DescriptorData) {
        binding.apply {
            when(descriptorData.format) {
                RvBleDeviceAdapter.Format.Text -> {}
                RvBleDeviceAdapter.Format.Bytes -> {}
                RvBleDeviceAdapter.Format.Integer -> {}
                RvBleDeviceAdapter.Format.Float -> {}
            }
        }
    }
}