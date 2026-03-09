package com.def4alt.executor.domain

import java.time.Instant

data class Job(
    val id: String,
    val script: String,
    val status: JobStatus,
    val requiredResources: ResourceSpec,
    val executorId: String? = null,
    val stdout: String? = null,
    val stderr: String? = null,
    val exitCode: Int? = null,
    val createdAt: Instant,
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
)
