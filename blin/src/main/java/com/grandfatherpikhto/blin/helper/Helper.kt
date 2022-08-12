package com.grandfatherpikhto.blin.helper

import android.bluetooth.BluetoothGattCharacteristic
import android.os.ParcelUuid
import com.grandfatherpikhto.blin.GenericUUIDs
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

fun Int.hasFlag(flag: Int) = this.and(flag) == flag
fun Int.addFlag(flag: Int) = this.or(flag)
fun Int.removeFlag(flag: Int) = this.and(flag.inv())

fun Byte.hasFlag(flag: Byte) = this.and(flag) == flag
fun Byte.addFlag(flag: Byte) = this.or(flag)
fun Byte.removeFlag(flag: Byte) = this.and(flag.inv())

fun ByteArray.toHexString(separator:String = ",") : String =
    joinToString (separator) { String.format("%02X", it) }


