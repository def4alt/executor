package com.def4alt.executor.pool

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PoolTargetCalculatorTest {
    private val calculator = PoolTargetCalculator()

    @Test
    fun `calculateCreateCount keeps the minimum warm capacity when no jobs are queued`() {
        val createCount = calculator.calculateCreateCount(
            policy = PoolPolicy(minReady = 2, maxReady = 5, maxBurstCreate = 3, idleTtlSeconds = 300),
            readyCount = 1,
            startingCount = 0,
            queuedCount = 0,
        )

        assertEquals(1, createCount)
    }

    @Test
    fun `calculateCreateCount grows pool with queue depth and respects burst cap`() {
        val createCount = calculator.calculateCreateCount(
            policy = PoolPolicy(minReady = 1, maxReady = 10, maxBurstCreate = 2, idleTtlSeconds = 300),
            readyCount = 0,
            startingCount = 0,
            queuedCount = 5,
        )

        assertEquals(2, createCount)
    }

    @Test
    fun `calculateCreateCount does not create pods when current capacity already meets target`() {
        val createCount = calculator.calculateCreateCount(
            policy = PoolPolicy(minReady = 2, maxReady = 4, maxBurstCreate = 3, idleTtlSeconds = 300),
            readyCount = 2,
            startingCount = 1,
            queuedCount = 1,
        )

        assertEquals(0, createCount)
    }
}
