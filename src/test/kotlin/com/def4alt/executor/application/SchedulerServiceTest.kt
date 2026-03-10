package com.def4alt.executor.application

import com.def4alt.executor.domain.Job
import com.def4alt.executor.domain.JobStatus
import com.def4alt.executor.domain.ResourceSpec
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchedulerServiceTest {
    @Test
    fun `scheduleNextQueuedJob launches the oldest unassigned queued job`() {
        val jobRepository = InMemorySchedulingJobRepository(
            mutableListOf(
                queuedJob(id = "job-2", createdAt = "2026-03-08T10:02:00Z"),
                queuedJob(id = "job-1", createdAt = "2026-03-08T10:00:00Z"),
            )
        )
        val launcher = RecordingJobLauncher()
        val service = SchedulerService(jobRepository, launcher) { "exec-1" }

        val launched = service.scheduleNextQueuedJob()

        assertTrue(launched)
        assertEquals(listOf(LaunchRequest("exec-1", "job-1")), launcher.launchedJobs)

        val storedJob = jobRepository.requireJob("job-1")
        assertEquals("exec-1", storedJob.executorId)
        assertEquals(JobStatus.QUEUED, storedJob.status)

        val untouchedJob = jobRepository.requireJob("job-2")
        assertNull(untouchedJob.executorId)
    }

    @Test
    fun `scheduleNextQueuedJob skips already assigned queued jobs and launches the next eligible job`() {
        val jobRepository = InMemorySchedulingJobRepository(
            mutableListOf(
                queuedJob(id = "job-1", createdAt = "2026-03-08T10:00:00Z", executorId = "exec-1"),
                queuedJob(id = "job-2", createdAt = "2026-03-08T10:01:00Z"),
            )
        )
        val launcher = RecordingJobLauncher()
        val service = SchedulerService(jobRepository, launcher) { "exec-2" }

        val launched = service.scheduleNextQueuedJob()

        assertTrue(launched)
        assertEquals(listOf(LaunchRequest("exec-2", "job-2")), launcher.launchedJobs)

        val stillAssignedJob = jobRepository.requireJob("job-1")
        assertEquals("exec-1", stillAssignedJob.executorId)

        val launchedJob = jobRepository.requireJob("job-2")
        assertEquals("exec-2", launchedJob.executorId)
    }

    @Test
    fun `scheduleNextQueuedJob launches only one job per scheduler tick`() {
        val jobRepository = InMemorySchedulingJobRepository(
            mutableListOf(
                queuedJob(id = "job-1", createdAt = "2026-03-08T10:00:00Z"),
                queuedJob(id = "job-2", createdAt = "2026-03-08T10:01:00Z"),
            )
        )
        val launcher = RecordingJobLauncher()
        val service = SchedulerService(jobRepository, launcher) { "exec-3" }

        val launched = service.scheduleNextQueuedJob()

        assertTrue(launched)
        assertEquals(listOf(LaunchRequest("exec-3", "job-1")), launcher.launchedJobs)
        assertEquals("exec-3", jobRepository.requireJob("job-1").executorId)
        assertNull(jobRepository.requireJob("job-2").executorId)
    }

    @Test
    fun `scheduleNextQueuedJob does nothing when no unassigned queued job exists`() {
        val jobRepository = InMemorySchedulingJobRepository(
            mutableListOf(
                queuedJob(id = "job-1", createdAt = "2026-03-08T10:00:00Z", executorId = "exec-1"),
                job(id = "job-2", status = JobStatus.FINISHED, createdAt = "2026-03-08T10:01:00Z"),
            )
        )
        val launcher = RecordingJobLauncher()
        val service = SchedulerService(jobRepository, launcher) { "exec-4" }

        val launched = service.scheduleNextQueuedJob()

        assertFalse(launched)
        assertTrue(launcher.launchedJobs.isEmpty())
        assertEquals("exec-1", jobRepository.requireJob("job-1").executorId)
        assertEquals(JobStatus.FINISHED, jobRepository.requireJob("job-2").status)
    }

    @Test
    fun `scheduleNextQueuedJob clears the claim when launcher creation fails`() {
        val jobRepository = InMemorySchedulingJobRepository(
            mutableListOf(queuedJob(id = "job-1", createdAt = "2026-03-08T10:00:00Z"))
        )
        val launcher = RecordingJobLauncher(shouldFail = true)
        val service = SchedulerService(jobRepository, launcher) { "exec-5" }

        val failure = kotlin.runCatching {
            service.scheduleNextQueuedJob()
        }.exceptionOrNull()

        assertEquals("launcher failed", failure?.message)
        assertNull(jobRepository.requireJob("job-1").executorId)
    }
}

private class InMemorySchedulingJobRepository(
    private val jobs: MutableList<Job>,
) : SchedulingJobRepository {
    override fun claimNextQueuedJob(executorId: String): Job? {
        val job = jobs.filter { it.status == JobStatus.QUEUED && it.executorId == null }.minByOrNull { it.createdAt } ?: return null
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

private data class LaunchRequest(
    val executorId: String,
    val jobId: String,
)

private class RecordingJobLauncher(
    private val shouldFail: Boolean = false,
) : ExecutorLauncher {
    val launchedJobs = mutableListOf<LaunchRequest>()

    override fun launch(executorId: String, job: Job) {
        launchedJobs += LaunchRequest(executorId = executorId, jobId = job.id)
        if (shouldFail) {
            throw IllegalStateException("launcher failed")
        }
    }
}

private fun queuedJob(
    id: String,
    createdAt: String,
    executorId: String? = null,
): Job = job(id = id, status = JobStatus.QUEUED, createdAt = createdAt, executorId = executorId)

private fun job(
    id: String,
    status: JobStatus,
    createdAt: String,
    executorId: String? = null,
): Job {
    return Job(
        id = id,
        script = "echo $id",
        status = status,
        requiredResources = ResourceSpec(cpus = 1, memory = 512),
        executorId = executorId,
        createdAt = Instant.parse(createdAt),
    )
}
