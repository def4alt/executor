package com.def4alt.executor.domain

import java.time.Instant

data class Executor(
    val id: String,
    val podName: String,
    val namespace: String,
    val status: ExecutorStatus,
    val jobId: String? = null,
    val createdAt: Instant,
    val readyAt: Instant? = null,
    val lastHeartbeatAt: Instant? = null,
    val leaseExpiresAt: Instant? = null,
)
