package com.grandfatherpikhto.blescan.helper

import android.bluetooth.BluetoothDevice
import com.grandfatherpikhto.blescan.model.BtLeDevice
import java.nio.ByteBuffer
import java.nio.ByteOrder

const val DEFAULT_NAME:String = "LED_STRIP"

/**
 * Network Byte Order
 * https://docs.microsoft.com/ru-ru/windows/win32/api/winsock/nf-winsock-htonl
 **/
private fun ByteArray.toHex():String {
    val out:StringBuilder = java.lang.StringBuilder()
    this.forEach { bt ->
        out.append(String.format("%02x", bt))
    }

    return out.toString()
}

private fun ByteArray.toInt(order: ByteOrder = ByteOrder.BIG_ENDIAN):Int {
    return ByteBuffer.wrap(this).order(order).getInt(0)
}

private fun ByteArray.toFloat(order: ByteOrder = ByteOrder.BIG_ENDIAN):Float {
    return ByteBuffer.wrap(this).order(order).getFloat(0)
}

/**
 * Преобразует число с плавающей запятой в байтовую последовательность для
 * передачи по сети (по стандарту BigEndian) https://en.wikipedia.org/wiki/Endianness
 */
fun Float.toByteArray(order: ByteOrder = ByteOrder.BIG_ENDIAN):ByteArray {
    return ByteBuffer.allocate(Float.SIZE_BYTES).order(order).putFloat(this).array();
}

/**
 * Преобразует целое число в байтовую последовательность для
 * передачи по сети (по стандарту BigEndian) https://en.wikipedia.org/wiki/Endianness
 */
fun Int.toByteArray(order: ByteOrder = ByteOrder.BIG_ENDIAN):ByteArray {
    return ByteBuffer.allocate(Int.SIZE_BYTES).order(order).putInt(this).array()
}

/**
 * Преобразует целое число в строку в виде 16-ричного беззнакового
 */
fun Int.toHex():String {
    return this.toUInt().toString(16)
}

fun BluetoothDevice.toBtLeDevice(defaultAddress: String = "00:00:00:00:00:00", defaultName:String = "Unknown Device"): BtLeDevice {
    return BtLeDevice(this.address ?: defaultAddress, this.name ?: defaultName, this.bondState)
}
