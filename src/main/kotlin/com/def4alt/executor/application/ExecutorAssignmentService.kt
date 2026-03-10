package com.def4alt.executor.application

import java.time.Clock
import java.time.Instant
import org.springframework.stereotype.Service

@Service
class ExecutorAssignmentService(
    private val jobRepository: SchedulingJobRepository,
    private val clock: Clock,
) {
    fun getAssignment(executorId: String): ExecutorAssignment? {
        val assignedJob = jobRepository.findAssignedJob(executorId) ?: return null
        val job = if (assignedJob.status == com.def4alt.executor.domain.JobStatus.QUEUED) {
            jobRepository.markInProgress(assignedJob.id, executorId, Instant.now(clock))
        } else {
            assignedJob
        }

        return ExecutorAssignment(
            jobId = job.id,
            script = job.script,
        )
    }
}

data class ExecutorAssignment(
    val jobId: String,
    val script: String,
)
