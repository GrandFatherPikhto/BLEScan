package com.grandfatherpikhto.blin.buffer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class MutableListQueue <T> constructor(val dispatcher: CoroutineDispatcher = Dispatchers.IO) : Queue<T> {
    private val buffer = mutableListOf<T>()
    override fun enqueue(element: T): Boolean = buffer.add(element)

    override fun dequeue(): T? {
        if (buffer.isNotEmpty()) {
            val first = buffer.first()
            buffer.removeFirst()
            return first
        }

        return null
    }

    override val count: Int
        get() = buffer.size

    override fun peek(): T? {
        if (buffer.isNotEmpty()) {
            return buffer.first()
        }

        return null
    }
}