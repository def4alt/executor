package com.def4alt.executor.application

import com.def4alt.executor.domain.Executor
import java.time.Instant

interface ExecutorRepository {
    fun create(executor: Executor)

    fun findById(id: String): Executor?

    fun markReady(executorId: String, readyAt: Instant): Executor

    fun markTerminated(executorId: String): Executor
}
