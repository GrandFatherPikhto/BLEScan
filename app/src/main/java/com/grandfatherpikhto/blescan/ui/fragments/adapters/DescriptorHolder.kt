package com.grandfatherpikhto.blescan.ui.fragments.adapters

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blescan.data.BleItem
import com.grandfatherpikhto.blescan.databinding.LayoutDescriptorBinding
import com.grandfatherpikhto.blescan.helper.dpToPx

class DescriptorHolder (view: View) : RecyclerView.ViewHolder(view) {

    private val binding = LayoutDescriptorBinding.bind(view)

    private var descriptorReadClickListener: ((BleItem, View) -> Unit)? = null
    private var descriptorFormatClickListener: ((BleItem, View) -> Unit)? = null

    private var _bleItem:BleItem? = null
    private val bleItem get() = _bleItem!!

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

    fun bind(item: BleItem) {
        _bleItem = item
        binding.apply {
            tvDescriptorUuid.text = bleItem.uuidDescriptor.toString().uppercase()

        }

        showDescriptor()
    }

    fun setOnDescriptorReadClickListener(listener: (BleItem, View) -> Unit) {
        descriptorReadClickListener = listener
    }

    fun setOnDescriptorValueFormatClickListener(listener: (BleItem, View) -> Unit) {
        descriptorFormatClickListener = listener
    }
}