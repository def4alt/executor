package com.def4alt.executor.application

import com.def4alt.executor.domain.ExecutorStatus
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class ExecutorPodCleanupService(
    private val executorRepository: ExecutorRepository,
    private val jobRepository: SchedulingJobRepository,
    private val executorRuntime: ExecutorRuntime,
    private val clock: Clock,
) {
    fun cleanup() {
        executorRepository.findByStatuses(setOf(ExecutorStatus.TERMINATED)).forEach {
            executorRuntime.deletePod(it.podName)
        }

        executorRuntime.findTerminatedPods().forEach {
            executorRepository.markTerminated(it.executorId)
            executorRuntime.deletePod(it.podName)
        }

        executorRuntime.findFailedPods().forEach {
            val executor = executorRepository.findById(it.executorId) ?: return@forEach
            if (executor.jobId != null) {
                jobRepository.markFailed(executor.jobId, "executor pod failed", Instant.now(clock))
            }
            executorRepository.markTerminated(it.executorId)
            executorRuntime.deletePod(it.podName)
        }
    }
}
