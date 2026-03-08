package com.def4alt.executor.application

import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class SchedulerService(
    private val jobRepository: SchedulingJobRepository,
    private val executorRepository: ExecutorRepository,
) {
    fun scheduleNextQueuedJob(now: Instant): JobAssignment? {
        val job = jobRepository.findNextQueuedJob() ?: return null
        val executor = executorRepository.leaseReadyExecutor(job.flavor, now.plus(LEASE_TTL)) ?: return null

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
    }
}

data class JobAssignment(
    val jobId: String,
    val executorId: String,
    val script: String,
)
