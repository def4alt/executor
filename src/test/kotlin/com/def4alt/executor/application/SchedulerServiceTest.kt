package com.def4alt.executor.application

import com.def4alt.executor.domain.Job
import com.def4alt.executor.domain.JobStatus
import com.def4alt.executor.domain.ResourceSpec
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
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
        val service = SchedulerService(jobRepository, launcher)

        val result = service.scheduleNextQueuedJob(Instant.parse("2026-03-08T10:05:00Z"))

        assertNull(result)
        assertEquals(listOf("job-1"), launcher.launchedJobs)

        val storedJob = jobRepository.requireJob("job-1")
        assertEquals("launched-job-1", storedJob.executorId)
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
        val service = SchedulerService(jobRepository, launcher)

        val result = service.scheduleNextQueuedJob(Instant.parse("2026-03-08T10:05:00Z"))

        assertNull(result)
        assertEquals(listOf("job-2"), launcher.launchedJobs)

        val stillAssignedJob = jobRepository.requireJob("job-1")
        assertEquals("exec-1", stillAssignedJob.executorId)

        val launchedJob = jobRepository.requireJob("job-2")
        assertEquals("launched-job-2", launchedJob.executorId)
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
        val service = SchedulerService(jobRepository, launcher)

        service.scheduleNextQueuedJob(Instant.parse("2026-03-08T10:05:00Z"))

        assertEquals(listOf("job-1"), launcher.launchedJobs)
        assertEquals("launched-job-1", jobRepository.requireJob("job-1").executorId)
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
        val service = SchedulerService(jobRepository, launcher)

        val result = service.scheduleNextQueuedJob(Instant.parse("2026-03-08T10:05:00Z"))

        assertNull(result)
        assertTrue(launcher.launchedJobs.isEmpty())
        assertEquals("exec-1", jobRepository.requireJob("job-1").executorId)
        assertEquals(JobStatus.FINISHED, jobRepository.requireJob("job-2").status)
    }
}

private class InMemorySchedulingJobRepository(
    private val jobs: MutableList<Job>,
) : SchedulingJobRepository {
    override fun findNextQueuedJob(): Job? = jobs.filter { it.status == JobStatus.QUEUED && it.executorId == null }.minByOrNull { it.createdAt }

    override fun findAssignedJob(executorId: String): Job? {
        return jobs.firstOrNull { it.executorId == executorId && it.status in setOf(JobStatus.QUEUED, JobStatus.IN_PROGRESS) }
    }

    override fun assignExecutor(jobId: String, executorId: String): Job {
        val index = jobs.indexOfFirst { it.id == jobId }
        val updated = jobs[index].copy(executorId = executorId)
        jobs[index] = updated
        return updated
    }

    override fun markFailed(jobId: String, stderr: String, finishedAt: Instant): Job {
        val index = jobs.indexOfFirst { it.id == jobId }
        val updated = jobs[index].copy(status = JobStatus.FAILED, stderr = stderr, finishedAt = finishedAt)
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

private class RecordingJobLauncher : ExecutorLauncher {
    val launchedJobs = mutableListOf<String>()

    override fun launch(job: Job): String {
        launchedJobs += job.id
        return "launched-${job.id}"
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
