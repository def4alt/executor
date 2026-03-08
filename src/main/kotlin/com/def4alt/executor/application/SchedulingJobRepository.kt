package com.def4alt.executor.application

import com.def4alt.executor.domain.Job
import java.time.Instant

interface SchedulingJobRepository {
    fun findNextQueuedJob(): Job?

    fun markInProgress(jobId: String, executorId: String, startedAt: Instant): Job
}
