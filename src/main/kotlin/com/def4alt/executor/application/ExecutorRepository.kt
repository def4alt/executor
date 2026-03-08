package com.def4alt.executor.application

import com.def4alt.executor.domain.Executor
import com.def4alt.executor.domain.ExecutorStatus
import java.time.Instant

interface ExecutorRepository {
    fun create(executor: Executor)

    fun findById(id: String): Executor?

    fun leaseReadyExecutor(flavor: String, leaseExpiresAt: Instant): Executor?

    fun countByFlavorAndStatuses(flavor: String, statuses: Set<ExecutorStatus>): Int

    fun markReady(executorId: String, readyAt: Instant): Executor

    fun attachJob(executorId: String, jobId: String): Executor

    fun markTerminated(executorId: String): Executor

    fun findByStatuses(statuses: Set<ExecutorStatus>): List<Executor>
}
