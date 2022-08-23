package com.grandfatherpikhto.blin.shadows

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import java.util.*
import kotlin.let
import kotlin.random.Random

@SuppressLint("PrivateApi")
@SuppressWarnings("unchecked")
@Implements(BluetoothGatt::class)
class ShadowBluetoothGatt {
    companion object {
        fun newInstance(bluetoothDevice: BluetoothDevice):BluetoothGatt
            = Shadow.newInstanceOf(BluetoothGatt::class.java,
//                arrayOf(
//                    iBluetoothGattClass,
//                    BluetoothDevice::class.java,
//                    Integer.TYPE,
//                    Boolean.TYPE,
//                    Integer.TYPE,
//                    Class.forName("android.content.AttributionSource")
//                ),
//                arrayOf<Any?>(null, device, 0, false, 0, null)
        ).let { bluetoothGatt ->
                bluetoothGatt
        }
    }

    private val mutableListServices = mutableListOf<BluetoothGattService>()

    init {
        (1..Random.nextInt(2,5)).forEach { _ ->
            val service = BluetoothGattService(UUID.randomUUID(), BluetoothGattService.SERVICE_TYPE_PRIMARY)
            (1..Random.nextInt(2,5)).forEach { _ ->
                val characteristic = BluetoothGattCharacteristic(UUID.randomUUID(),
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY
                        .or(BluetoothGattCharacteristic.PROPERTY_READ)
                        .or(BluetoothGattCharacteristic.PROPERTY_WRITE),
                    0
                    )
                service.addCharacteristic(characteristic)
            }
            mutableListServices.add(service)
        }
    }

    val services: ArrayList<BluetoothGattService>
        get() = mutableListServices as ArrayList<BluetoothGattService>

    fun getService(uuid: UUID) : BluetoothGattService? =
        mutableListServices.find { it.uuid == uuid }
}