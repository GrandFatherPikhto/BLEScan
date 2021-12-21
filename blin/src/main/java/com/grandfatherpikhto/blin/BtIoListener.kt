package com.grandfatherpikhto.blin

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*

@DelicateCoroutinesApi
@InternalCoroutinesApi
interface BtIoListener {
    fun onBindBtInputQueue(btOutputQueue: BtOutputQueue?) {}
    fun onBindBtOutputQueue(btInputQueue: BtInputQueue?) {}
    fun onCharacteristicReaded(charUuid: UUID) {}
    fun onDescriptorReaded(charUuid: UUID, descrUuid: UUID) {}
    fun onCharacteristicWrited(charUuid: UUID) {}
    fun onDescriptorWrited(charUuid: UUID, descrUuid: UUID) {}
}