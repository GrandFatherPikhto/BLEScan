package com.grandfatherpikhto.blescan.service

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@DelicateCoroutinesApi
@InternalCoroutinesApi
class BluetoothInterfaceLazy: Lazy<BluetoothInterface> {
    companion object Instance {
        private var instance: BluetoothInterface? = null
        const val TAG:String = "BluetoothInterface"
        fun getInstance():BluetoothInterface  {
            instance?.let {
                return instance!!
            }

            instance = BluetoothInterface()
            return instance!!
        }
    }

    override val value: BluetoothInterface
        get() = getInstance()

    override fun isInitialized(): Boolean {
        return instance != null
    }
}