package com.def4alt.executor.application

import com.def4alt.executor.domain.Job
import java.time.Instant

interface SchedulingJobRepository {
    fun findNextQueuedJob(): Job?

    fun findAssignedJob(executorId: String): Job?

    fun markFailed(jobId: String, stderr: String, finishedAt: Instant): Job

    fun markInProgress(jobId: String, executorId: String, startedAt: Instant): Job
}
