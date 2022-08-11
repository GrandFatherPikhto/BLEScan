package com.grandfatherpikhto.blescan.ui.adapters

import android.bluetooth.BluetoothGattService
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.blin.GenericUUIDs.genericName
import com.grandfatherpikhto.blin.GenericUUIDs.genericStringUUID
import com.grandfatherpikhto.blin.helper.hasFlag
import com.grandfatherpikhto.blescan.R
import com.grandfatherpikhto.blescan.data.DescriptorData
import com.grandfatherpikhto.blescan.data.ServiceData
import com.grandfatherpikhto.blescan.databinding.LayoutServiceBinding

class ServiceHolder (private val view: View) : RecyclerView.ViewHolder(view) {

    private val binding = LayoutServiceBinding.bind(view)

    private val tagLog = this.javaClass.simpleName

    private fun getString(resId: Int, vararg formatArgs: String) = view.context.getString(resId, formatArgs)

    private fun getTextServiceType(bluetoothGattService: BluetoothGattService) : String? =
        if (bluetoothGattService.type.hasFlag(BluetoothGattService.SERVICE_TYPE_PRIMARY))
            getString(R.string.service_primary)
        else if (bluetoothGattService.type.hasFlag(BluetoothGattService.SERVICE_TYPE_SECONDARY))
            getString(R.string.service_secondary)
        else null


    fun bind(serviceData: ServiceData) {
        binding.apply {
            tvServiceName.text = serviceData.bluetoothGattService
                .uuid.genericName(getString(R.string.custom_service))
            tvServiceUuid.text = serviceData.bluetoothGattService
                .uuid.genericStringUUID()

            tvServiceType.text = getTextServiceType(serviceData.bluetoothGattService) ?: ""

                serviceData.bluetoothGattService.uuid.toString().uppercase()
            if (serviceData.opened) {
                ivUpDown.setImageResource(R.drawable.ic_up)
            } else {
                ivUpDown.setImageResource(R.drawable.ic_down)
            }
        }
    }
}