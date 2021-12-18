package com.grandfatherpikhto.blescan.helper

import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
import com.grandfatherpikhto.blescan.model.BtLeDevice
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

const val DEFAULT_NAME:String = "LED_STRIP"

const val BASE_UUID:String = "00000000-0000-1000-8000-00805F9B34FB"

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

/**
 * Выделить 16-битный идентификатор из UUID
 */
fun ParcelUuid.to16():Int {
    return this.uuid.to16()
}

fun UUID.to16():Int {
    return this.mostSignificantBits.shr(32).and(0xFFFF).toInt()
}

/**
 * Проверяет, является ли UUID-сервиса Основным (Generic)
 */
fun ParcelUuid.isGeneric():Boolean {
    return this.uuid.isGeneric()
}

/**
 *
 */
fun UUID.isGeneric():Boolean {
    return this.mostSignificantBits and -0xffff00000001L == 0x1000L
}
