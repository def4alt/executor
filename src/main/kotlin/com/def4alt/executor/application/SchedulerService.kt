package com.def4alt.executor.application

import java.util.UUID

class SchedulerService(
    private val jobRepository: SchedulingJobRepository,
    private val executorLauncher: ExecutorLauncher,
    private val executorIdGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    fun scheduleNextQueuedJob(): Boolean {
        val executorId = executorIdGenerator()
        val job = jobRepository.claimNextQueuedJob(executorId) ?: return false

        try {
            executorLauncher.launch(executorId, job)
        } catch (exception: Exception) {
            jobRepository.clearExecutorAssignment(job.id)
            throw exception
        }

        return true
    }
}
