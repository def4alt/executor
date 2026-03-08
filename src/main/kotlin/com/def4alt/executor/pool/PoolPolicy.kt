package com.def4alt.executor.pool

data class PoolPolicy(
    val minReady: Int,
    val maxReady: Int,
    val maxBurstCreate: Int,
    val idleTtlSeconds: Int,
)
