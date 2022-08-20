package com.grandfatherpikhto.blin.buffer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class QueueBufferTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    private val dispatcher = UnconfinedTestDispatcher()

    private val queueBuffer = QueueBuffer(dispatcher)

    @Test
    fun testQueue() = runTest (dispatcher) {

    }
}