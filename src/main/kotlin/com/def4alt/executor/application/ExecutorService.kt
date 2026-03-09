package com.def4alt.executor.application

import com.def4alt.executor.domain.Executor
import com.def4alt.executor.domain.ExecutorStatus
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class ExecutorService(
    private val executorRepository: ExecutorRepository,
    private val clock: Clock,
) {
    fun register(request: RegisterExecutorCommand): Executor {
        val now = Instant.now(clock)
        val existing = executorRepository.findById(request.id)
        if (existing == null) {
            executorRepository.create(
                Executor(
                    id = request.id,
                    podName = request.podName,
                    namespace = request.namespace,
                    status = ExecutorStatus.STARTING,
                    createdAt = now,
                )
            )
        }

        return executorRepository.markReady(request.id, now)
    }
}

data class RegisterExecutorCommand(
    val id: String,
    val podName: String,
    val namespace: String,
)
