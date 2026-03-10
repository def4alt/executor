package com.def4alt.executor.application

import java.time.Instant
import java.util.UUID

class SchedulerService(
    private val jobRepository: SchedulingJobRepository,
    private val executorLauncher: ExecutorLauncher,
    private val executorIdGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    fun scheduleNextQueuedJob(now: Instant): JobAssignment? {
        val executorId = executorIdGenerator()
        val job = jobRepository.claimNextQueuedJob(executorId) ?: return null

        try {
            executorLauncher.launch(executorId, job)
        } catch (exception: Exception) {
            jobRepository.clearExecutorAssignment(job.id)
            throw exception
        }

        return null
    }
}

data class JobAssignment(
    val jobId: String,
    val executorId: String,
    val script: String,
)
