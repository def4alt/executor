package com.def4alt.executor.application

import com.def4alt.executor.domain.Job
import com.def4alt.executor.domain.JobStatus
import com.def4alt.executor.domain.ResourceSpec
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExecutorAssignmentServiceTest {
    private val fixedNow = Instant.parse("2026-03-08T10:05:00Z")
    private val clock: Clock = Clock.fixed(fixedNow, ZoneOffset.UTC)

    @Test
    fun `getAssignment marks queued jobs in progress on first pickup`() {
        val repository = InMemoryAssignmentJobRepository(
            mutableListOf(assignedJob(id = "job-1", executorId = "exec-1", status = JobStatus.QUEUED))
        )
        val service = ExecutorAssignmentService(repository, clock)

        val assignment = service.getAssignment("exec-1")

        assertNotNull(assignment)
        assertEquals("job-1", assignment.jobId)
        assertEquals("echo job-1", assignment.script)

        val storedJob = repository.requireJob("job-1")
        assertEquals(JobStatus.IN_PROGRESS, storedJob.status)
        assertEquals(fixedNow, storedJob.startedAt)
    }

    @Test
    fun `getAssignment leaves already in-progress jobs unchanged`() {
        val startedAt = Instant.parse("2026-03-08T10:04:00Z")
        val repository = InMemoryAssignmentJobRepository(
            mutableListOf(assignedJob(id = "job-2", executorId = "exec-2", status = JobStatus.IN_PROGRESS, startedAt = startedAt))
        )
        val service = ExecutorAssignmentService(repository, clock)

        val assignment = service.getAssignment("exec-2")

        assertNotNull(assignment)
        val storedJob = repository.requireJob("job-2")
        assertEquals(JobStatus.IN_PROGRESS, storedJob.status)
        assertEquals(startedAt, storedJob.startedAt)
    }

    @Test
    fun `getAssignment returns null when executor has no assigned job`() {
        val repository = InMemoryAssignmentJobRepository(mutableListOf())
        val service = ExecutorAssignmentService(repository, clock)

        val assignment = service.getAssignment("exec-3")

        assertNull(assignment)
    }

}

private class InMemoryAssignmentJobRepository(
    private val jobs: MutableList<Job>,
) : SchedulingJobRepository {
    override fun claimNextQueuedJob(executorId: String): Job? {
        val job = jobs.firstOrNull { it.status == JobStatus.QUEUED && it.executorId == null } ?: return null
        val index = jobs.indexOfFirst { it.id == job.id }
        val updated = jobs[index].copy(executorId = executorId)
        jobs[index] = updated
        return updated
    }

    override fun findAssignedJob(executorId: String): Job? {
        return jobs.firstOrNull { it.executorId == executorId && it.status in setOf(JobStatus.QUEUED, JobStatus.IN_PROGRESS) }
    }

    override fun clearExecutorAssignment(jobId: String): Job {
        val index = jobs.indexOfFirst { it.id == jobId }
        val updated = jobs[index].copy(executorId = null)
        jobs[index] = updated
        return updated
    }
    override fun markInProgress(jobId: String, executorId: String, startedAt: Instant): Job {
        val index = jobs.indexOfFirst { it.id == jobId }
        val updated = jobs[index].copy(status = JobStatus.IN_PROGRESS, executorId = executorId, startedAt = startedAt)
        jobs[index] = updated
        return updated
    }

    fun requireJob(jobId: String): Job = jobs.first { it.id == jobId }
}

private fun assignedJob(
    id: String,
    executorId: String,
    status: JobStatus,
    script: String = "echo $id",
    startedAt: Instant? = null,
): Job {
    return Job(
        id = id,
        script = script,
        status = status,
        requiredResources = ResourceSpec(cpus = 1, memory = 256),
        executorId = executorId,
        createdAt = Instant.parse("2026-03-08T10:00:00Z"),
        startedAt = startedAt,
    )
}
