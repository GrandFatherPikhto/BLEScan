package com.grandfatherpikhto.blin

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@DelicateCoroutinesApi
@InternalCoroutinesApi
class BtIoInterfaceLazy: Lazy<BtIoInterface> {
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

    override val value: BtIoInterface
        get() = getInstance()

    override fun isInitialized(): Boolean {
        return instance != null
    }
}