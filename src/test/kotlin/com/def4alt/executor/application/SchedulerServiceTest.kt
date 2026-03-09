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
    fun `scheduleNextQueuedJob launches a new executor for an unassigned queued job`() {
        val jobRepository = InMemorySchedulingJobRepository(
            mutableListOf(
                Job(
                    id = "job-1",
                    script = "echo run",
                    status = JobStatus.QUEUED,
                    requiredResources = ResourceSpec(cpus = 1, memory = 512),
                    createdAt = Instant.parse("2026-03-08T10:00:00Z"),
                )
            )
        )
        val launcher = RecordingJobLauncher()
        val service = SchedulerService(jobRepository, launcher)

        val assignment = service.scheduleNextQueuedJob(Instant.parse("2026-03-08T10:01:00Z"))

        assertNull(assignment)
        assertEquals(listOf("job-1"), launcher.launchedJobs)
        assertEquals("launched-job-1", jobRepository.jobs.single().executorId)
        assertEquals(JobStatus.QUEUED, jobRepository.jobs.single().status)
    }

    @Test
    fun `scheduleNextQueuedJob does nothing when queued job already has an executor`() {
        val jobRepository = InMemorySchedulingJobRepository(
            mutableListOf(
                Job(
                    id = "job-1",
                    script = "echo wait",
                    status = JobStatus.QUEUED,
                    requiredResources = ResourceSpec(cpus = 1, memory = 512),
                    executorId = "exec-1",
                    createdAt = Instant.parse("2026-03-08T10:00:00Z"),
                )
            )
        )
        val launcher = RecordingJobLauncher()
        val service = SchedulerService(jobRepository, launcher)

        val assignment = service.scheduleNextQueuedJob(Instant.parse("2026-03-08T10:01:00Z"))

        assertNull(assignment)
        assertTrue(launcher.launchedJobs.isEmpty())
    }
}

private class InMemorySchedulingJobRepository(
    val jobs: MutableList<Job>,
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
}

private class RecordingJobLauncher : ExecutorLauncher {
    val launchedJobs = mutableListOf<String>()

    override fun launch(job: Job): String {
        launchedJobs += job.id
        return "launched-${job.id}"
    }
}
