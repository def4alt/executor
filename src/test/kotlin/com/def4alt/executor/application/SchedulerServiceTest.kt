package com.def4alt.executor.application

import com.def4alt.executor.domain.Executor
import com.def4alt.executor.domain.ExecutorStatus
import com.def4alt.executor.domain.Job
import com.def4alt.executor.domain.JobStatus
import com.def4alt.executor.domain.ResourceSpec
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SchedulerServiceTest {
    @Test
    fun `scheduleNextQueuedJob leases a ready executor and starts the job`() {
        val jobRepository = InMemorySchedulingJobRepository(
            mutableListOf(
                Job(
                    id = "job-1",
                    script = "echo run",
                    status = JobStatus.QUEUED,
                    requiredResources = ResourceSpec(cpus = 1, memory = 512),
                    flavor = "small-linux",
                    createdAt = Instant.parse("2026-03-08T10:00:00Z"),
                )
            )
        )
        val executorRepository = InMemoryExecutorRepository(
            mutableListOf(
                Executor(
                    id = "exec-1",
                    podName = "executor-small-1",
                    namespace = "executor-system",
                    flavor = "small-linux",
                    status = ExecutorStatus.READY,
                    createdAt = Instant.parse("2026-03-08T09:59:00Z"),
                    readyAt = Instant.parse("2026-03-08T09:59:30Z"),
                )
            )
        )
        val service = SchedulerService(jobRepository, executorRepository)

        val assignment = service.scheduleNextQueuedJob(Instant.parse("2026-03-08T10:01:00Z"))

        assertEquals("job-1", assignment?.jobId)
        assertEquals("exec-1", assignment?.executorId)
        assertEquals(JobStatus.IN_PROGRESS, jobRepository.jobs.single().status)
        assertEquals("exec-1", jobRepository.jobs.single().executorId)
        assertEquals(ExecutorStatus.LEASED, executorRepository.executors.single().status)
        assertEquals("job-1", executorRepository.executors.single().jobId)
    }

    @Test
    fun `scheduleNextQueuedJob leaves job queued when no executor is ready`() {
        val jobRepository = InMemorySchedulingJobRepository(
            mutableListOf(
                Job(
                    id = "job-1",
                    script = "echo wait",
                    status = JobStatus.QUEUED,
                    requiredResources = ResourceSpec(cpus = 1, memory = 512),
                    flavor = "small-linux",
                    createdAt = Instant.parse("2026-03-08T10:00:00Z"),
                )
            )
        )
        val executorRepository = InMemoryExecutorRepository(mutableListOf())
        val service = SchedulerService(jobRepository, executorRepository)

        val assignment = service.scheduleNextQueuedJob(Instant.parse("2026-03-08T10:01:00Z"))

        assertNull(assignment)
        assertEquals(JobStatus.QUEUED, jobRepository.jobs.single().status)
    }
}

private class InMemorySchedulingJobRepository(
    val jobs: MutableList<Job>,
) : SchedulingJobRepository {
    override fun findNextQueuedJob(): Job? = jobs.filter { it.status == JobStatus.QUEUED }.minByOrNull { it.createdAt }

    override fun markInProgress(jobId: String, executorId: String, startedAt: Instant): Job {
        val index = jobs.indexOfFirst { it.id == jobId }
        val updated = jobs[index].copy(status = JobStatus.IN_PROGRESS, executorId = executorId, startedAt = startedAt)
        jobs[index] = updated
        return updated
    }
}

private class InMemoryExecutorRepository(
    val executors: MutableList<Executor>,
) : ExecutorRepository {
    override fun create(executor: Executor) {
        executors += executor
    }

    override fun findById(id: String): Executor? = executors.firstOrNull { it.id == id }

    override fun leaseReadyExecutor(flavor: String, leaseExpiresAt: Instant): Executor? {
        val index = executors.indexOfFirst { it.flavor == flavor && it.status == ExecutorStatus.READY }
        if (index == -1) {
            return null
        }

        val leased = executors[index].copy(status = ExecutorStatus.LEASED, leaseExpiresAt = leaseExpiresAt)
        executors[index] = leased
        return leased
    }

    override fun markReady(executorId: String, readyAt: Instant): Executor {
        val index = executors.indexOfFirst { it.id == executorId }
        val updated = executors[index].copy(status = ExecutorStatus.READY, readyAt = readyAt)
        executors[index] = updated
        return updated
    }

    override fun attachJob(executorId: String, jobId: String): Executor {
        val index = executors.indexOfFirst { it.id == executorId }
        val updated = executors[index].copy(jobId = jobId)
        executors[index] = updated
        return updated
    }

    override fun markTerminated(executorId: String): Executor {
        val index = executors.indexOfFirst { it.id == executorId }
        val updated = executors[index].copy(status = ExecutorStatus.TERMINATED)
        executors[index] = updated
        return updated
    }
}
