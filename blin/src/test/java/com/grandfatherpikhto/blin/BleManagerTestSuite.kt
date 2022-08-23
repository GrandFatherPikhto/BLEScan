package com.grandfatherpikhto.blin

import com.grandfatherpikhto.blin.buffer.QueueBufferTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    BleScanManagerTest::class,
    BleGattManagerTest::class,
    BleBondManagerTest::class,
    QueueBufferTest::class
)

class BleManagerTestSuite