package com.grandfatherpikhto.blin

import android.os.ParcelUuid
import java.util.*

/**
 * Выделить 16-битный идентификатор из generic UUID
 */
fun ParcelUuid.to16():Int {
    return this.uuid.to16()
}

/**
 * Возвращает 16-битное представление generic UUID
 */
fun UUID.to16():Int {
    return this.mostSignificantBits.shr(32).and(0xFFFF).toInt()
}

/**
 * Проверяет, является ли ParcelUUID -- Основным (Generic)
 */
fun ParcelUuid.isGeneric():Boolean {
    return this.uuid.isGeneric()
}

/**
 * Проверяет, является ли UUID -- Основным (generic)
 */
fun UUID.isGeneric():Boolean {
    return this.mostSignificantBits and -0xffff00000001L == 0x1000L
}
