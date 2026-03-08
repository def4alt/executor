package com.def4alt.executor.application

import com.def4alt.executor.persistence.JdbcJobRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class ExecutorResultService(
    private val jobRepository: JdbcJobRepository,
    private val executorRepository: ExecutorRepository,
    private val clock: Clock,
) {
    fun recordResult(executorId: String, request: ExecutorResultCommand) {
        jobRepository.markFinished(
            jobId = request.jobId,
            stdout = request.stdout,
            stderr = request.stderr,
            exitCode = request.exitCode,
            finishedAt = Instant.now(clock),
        )
        executorRepository.markTerminated(executorId)
    }
}

data class ExecutorResultCommand(
    val jobId: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
)
