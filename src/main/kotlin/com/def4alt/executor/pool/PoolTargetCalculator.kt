package com.def4alt.executor.pool

import kotlin.math.max
import kotlin.math.min

class PoolTargetCalculator {
    fun calculateCreateCount(
        policy: PoolPolicy,
        readyCount: Int,
        startingCount: Int,
        queuedCount: Int,
    ): Int {
        val targetReady = min(policy.maxReady, policy.minReady + queuedCount)
        val missingCapacity = max(0, targetReady - (readyCount + startingCount))

        return min(policy.maxBurstCreate, missingCapacity)
    }
}
