package com.def4alt.executor.application

import com.def4alt.executor.domain.ExecutorStatus
import java.time.Duration
import java.time.Instant

class SchedulerService(
    private val jobRepository: SchedulingJobRepository,
    private val executorRepository: ExecutorRepository,
    private val executorLauncher: ExecutorLauncher,
) {
    fun scheduleNextQueuedJob(now: Instant): JobAssignment? {
        val job = jobRepository.findNextQueuedJob() ?: return null
        val executor = executorRepository.leaseReadyExecutor(job.flavor, now.plus(LEASE_TTL))
        if (executor == null) {
            val activeExecutorCount = executorRepository.countByFlavorAndStatuses(job.flavor, ACTIVE_STATUSES)
            if (activeExecutorCount == 0) {
                executorLauncher.launch(job.flavor)
            }
            return null
        }

        executorRepository.attachJob(executor.id, job.id)
        val startedJob = jobRepository.markInProgress(job.id, executor.id, now)

        return JobAssignment(
            jobId = startedJob.id,
            executorId = executor.id,
            script = startedJob.script,
        )
    }

    companion object {
        private val LEASE_TTL: Duration = Duration.ofMinutes(5)
        private val ACTIVE_STATUSES = setOf(
            ExecutorStatus.STARTING,
            ExecutorStatus.READY,
            ExecutorStatus.LEASED,
            ExecutorStatus.RUNNING,
        )
    }
}

data class JobAssignment(
    val jobId: String,
    val executorId: String,
    val script: String,
)
