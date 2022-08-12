package com.grandfatherpikhto.blescan.data

import android.bluetooth.BluetoothGattDescriptor
import com.grandfatherpikhto.blescan.ui.fragments.adapters.RvBleDeviceAdapter

data class DescriptorData (var bluetoothGattDescriptor: BluetoothGattDescriptor,
                           var format: RvBleDeviceAdapter.Format = RvBleDeviceAdapter.Format.Bytes,
                           var visible: Boolean = false)