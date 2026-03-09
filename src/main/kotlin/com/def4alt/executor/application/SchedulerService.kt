package com.def4alt.executor.application

import com.def4alt.executor.domain.ExecutorStatus
import java.time.Duration
import java.time.Instant

class SchedulerService(
    private val jobRepository: SchedulingJobRepository,
    private val executorLauncher: ExecutorLauncher,
) {
    fun scheduleNextQueuedJob(now: Instant): JobAssignment? {
        val job = jobRepository.findNextQueuedJob() ?: return null
        if (job.executorId != null) {
            return null
        }

        val executorId = executorLauncher.launch(job)
        jobRepository.assignExecutor(job.id, executorId)
        return null
    }
}

data class JobAssignment(
    val jobId: String,
    val executorId: String,
    val script: String,
)
