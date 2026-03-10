package com.def4alt.executor.application

import java.time.Clock
import java.time.Instant
import java.util.Base64
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

    fun getAssignmentScript(executorId: String): String? {
        val assignment = getAssignment(executorId) ?: return null
        val scriptBase64 = Base64.getEncoder().encodeToString(assignment.script.toByteArray())

        return "JOB_ID=${assignment.jobId}\nSCRIPT_BASE64=$scriptBase64\n"
    }
}

data class ExecutorAssignment(
    val jobId: String,
    val script: String,
)
