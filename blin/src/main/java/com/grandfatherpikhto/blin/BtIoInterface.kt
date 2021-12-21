package com.grandfatherpikhto.blin

import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

@InternalCoroutinesApi
@DelicateCoroutinesApi
class BtIoInterface {
    companion object Instance {
        private var instance: BtIoInterface? = null
        const val TAG:String = "BtIoInterface"
        fun getInstance(): BtIoInterface {
            instance?.let {
                return instance!!
            }
            instance = BtIoInterface()
            return instance!!
        }
    }

    operator fun getValue(
        owner: Any?,
        property: KProperty<*>
    ): BtIoInterface {
        return getInstance()
    }

    private val btIoListeners: MutableList<BtIoListener> = mutableListOf()

    var btInputQueue: BtInputQueue? by Delegates.observable(null) { _, _, outputQueue ->
        btIoListeners.forEach { listener ->
            listener.onBindBtOutputQueue(outputQueue)
        }
    }

    var btOutputQueue: BtOutputQueue? by Delegates.observable(null) { _, _, inputQueue ->
        btIoListeners.forEach { listener ->
            listener.onBindBtInputQueue(inputQueue)
        }
    }

    fun characteristicReaded(uuid: UUID) {
        Log.d(TAG, "characteristicReaded: $uuid, listeners: ${btIoListeners.size}")
        btIoListeners.forEach { listener ->
            listener.onCharacteristicReaded(uuid)
        }
    }

    fun addListener(listener: BtIoListener) {
        if(!btIoListeners.contains(listener)) {
            btIoListeners.add(listener)
        }

        // Thread.dumpStack()
        // Thread.getAllStackTraces()
    }

    fun removeListener(listener: BtIoListener): Boolean {
        return btIoListeners.remove(listener)
    }

    /**
     *
     */
    fun writeCharacteristic(uuid: UUID, value: ByteArray, last:Boolean = false) = btOutputQueue?.writeCharacteristic(uuid, value, last)
    fun writeDescriptor(charUuid: UUID, descrUuid: UUID, value: ByteArray, last:Boolean = false) = btOutputQueue?.writeDescriptor(charUuid, descrUuid, value, last)
    fun readCharacteristic(uuid: UUID, last:Boolean = false) = btInputQueue?.readCharacteristic(uuid, last)
    fun readDescriptor(charUuid: UUID, descrUuid: UUID, last:Boolean = false) = btInputQueue?.readDescriptor(charUuid, descrUuid, last)
    fun requestCharacteristic(charUuid: UUID) = btInputQueue?.requestCharacteristic(charUuid)
}