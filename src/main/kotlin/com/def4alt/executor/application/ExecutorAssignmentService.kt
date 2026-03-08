package com.def4alt.executor.application

import org.springframework.stereotype.Service
import java.util.Base64

@Service
class ExecutorAssignmentService(
    private val jobRepository: SchedulingJobRepository,
) {
    fun getAssignment(executorId: String): ExecutorAssignment? {
        val job = jobRepository.findAssignedJob(executorId) ?: return null

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
