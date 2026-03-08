package com.def4alt.executor.application

import com.def4alt.executor.domain.Executor
import com.def4alt.executor.domain.ExecutorStatus
import com.def4alt.executor.domain.Job
import com.def4alt.executor.domain.JobStatus
import com.def4alt.executor.domain.ResourceSpec
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class ExecutorPodCleanupServiceTest {
    @Test
    fun `cleanup deletes terminated executor pods`() {
        val executorRepository = CleanupExecutorRepository(
            mutableListOf(
                Executor(
                    id = "exec-1",
                    podName = "executor-small-1",
                    namespace = "executor",
                    flavor = "small-linux",
                    status = ExecutorStatus.TERMINATED,
                    createdAt = Instant.parse("2026-03-08T10:00:00Z"),
                )
            )
        )
        val jobRepository = CleanupJobRepository(mutableListOf())
        val runtime = FakeExecutorRuntime(
            terminatedExecutors = emptyList(),
            failedExecutors = emptyList(),
        )
        val service = ExecutorPodCleanupService(executorRepository, jobRepository, runtime, FIXED_CLOCK)

        service.cleanup()

        assertEquals(listOf("executor-small-1"), runtime.deletedPods)
    }

    @Test
    fun `cleanup marks failed executor job as failed and deletes the pod`() {
        val executorRepository = CleanupExecutorRepository(
            mutableListOf(
                Executor(
                    id = "exec-1",
                    podName = "executor-small-1",
                    namespace = "executor",
                    flavor = "small-linux",
                    status = ExecutorStatus.LEASED,
                    jobId = "job-1",
                    createdAt = Instant.parse("2026-03-08T10:00:00Z"),
                )
            )
        )
        val jobRepository = CleanupJobRepository(
            mutableListOf(
                Job(
                    id = "job-1",
                    script = "echo hello",
                    status = JobStatus.IN_PROGRESS,
                    requiredResources = ResourceSpec(cpus = 1, memory = 128),
                    flavor = "small-linux",
                    executorId = "exec-1",
                    createdAt = Instant.parse("2026-03-08T10:00:00Z"),
                    startedAt = Instant.parse("2026-03-08T10:01:00Z"),
                )
            )
        )
        val runtime = FakeExecutorRuntime(
            terminatedExecutors = emptyList(),
            failedExecutors = listOf(FailedExecutorPod(executorId = "exec-1", podName = "executor-small-1")),
        )
        val service = ExecutorPodCleanupService(executorRepository, jobRepository, runtime, FIXED_CLOCK)

        service.cleanup()

        assertEquals(ExecutorStatus.TERMINATED, executorRepository.executors.single().status)
        assertEquals(JobStatus.FAILED, jobRepository.jobs.single().status)
        assertEquals(listOf("executor-small-1"), runtime.deletedPods)
    }
}

private class CleanupExecutorRepository(
    val executors: MutableList<Executor>,
) : ExecutorRepository {
    override fun create(executor: Executor) { executors += executor }
    override fun findById(id: String): Executor? = executors.firstOrNull { it.id == id }
    override fun leaseReadyExecutor(flavor: String, leaseExpiresAt: Instant): Executor? = null
    override fun countByFlavorAndStatuses(flavor: String, statuses: Set<ExecutorStatus>): Int = executors.count { it.flavor == flavor && it.status in statuses }
    override fun markReady(executorId: String, readyAt: Instant): Executor = update(executorId) { it.copy(status = ExecutorStatus.READY, readyAt = readyAt) }
    override fun attachJob(executorId: String, jobId: String): Executor = update(executorId) { it.copy(jobId = jobId) }
    override fun markTerminated(executorId: String): Executor = update(executorId) { it.copy(status = ExecutorStatus.TERMINATED) }
    override fun findByStatuses(statuses: Set<ExecutorStatus>): List<Executor> = executors.filter { it.status in statuses }

    private fun update(executorId: String, block: (Executor) -> Executor): Executor {
        val index = executors.indexOfFirst { it.id == executorId }
        val updated = block(executors[index])
        executors[index] = updated
        return updated
    }
}

private class CleanupJobRepository(
    val jobs: MutableList<Job>,
) : SchedulingJobRepository {
    override fun findNextQueuedJob(): Job? = jobs.firstOrNull { it.status == JobStatus.QUEUED }
    override fun findAssignedJob(executorId: String): Job? = jobs.firstOrNull { it.executorId == executorId && it.status == JobStatus.IN_PROGRESS }
    override fun markInProgress(jobId: String, executorId: String, startedAt: Instant): Job = update(jobId) { it.copy(status = JobStatus.IN_PROGRESS, executorId = executorId, startedAt = startedAt) }
    override fun markFailed(jobId: String, stderr: String, finishedAt: Instant): Job = update(jobId) { it.copy(status = JobStatus.FAILED, stderr = stderr, finishedAt = finishedAt) }

    private fun update(jobId: String, block: (Job) -> Job): Job {
        val index = jobs.indexOfFirst { it.id == jobId }
        val updated = block(jobs[index])
        jobs[index] = updated
        return updated
    }
}

private class FakeExecutorRuntime(
    private val terminatedExecutors: List<TerminatedExecutorPod>,
    private val failedExecutors: List<FailedExecutorPod>,
) : ExecutorRuntime {
    val deletedPods = mutableListOf<String>()

    override fun findTerminatedPods(): List<TerminatedExecutorPod> = terminatedExecutors
    override fun findFailedPods(): List<FailedExecutorPod> = failedExecutors
    override fun deletePod(podName: String) {
        deletedPods += podName
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(Instant.parse("2026-03-08T10:07:00Z"), ZoneOffset.UTC)
