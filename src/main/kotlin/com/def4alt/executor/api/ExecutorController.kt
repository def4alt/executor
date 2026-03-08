package com.def4alt.executor.api

import com.def4alt.executor.application.ExecutorResultCommand
import com.def4alt.executor.application.ExecutorResultService
import com.def4alt.executor.application.ExecutorService
import com.def4alt.executor.application.RegisterExecutorCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/internal/executors")
class ExecutorController(
    private val executorService: ExecutorService,
    private val executorResultService: ExecutorResultService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterExecutorRequest): ExecutorResponse {
        val executor = executorService.register(
            RegisterExecutorCommand(
                id = request.id,
                podName = request.podName,
                namespace = request.namespace,
                flavor = request.flavor,
            )
        )

        return ExecutorResponse(
            id = executor.id,
            podName = executor.podName,
            namespace = executor.namespace,
            flavor = executor.flavor,
            status = executor.status.name,
            jobId = executor.jobId,
            readyAt = executor.readyAt,
            lastHeartbeatAt = executor.lastHeartbeatAt,
        )
    }

    @PostMapping("/{id}/result")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun recordResult(@PathVariable id: String, @Valid @RequestBody request: ExecutorResultRequest) {
        executorResultService.recordResult(
            executorId = id,
            request = ExecutorResultCommand(
                jobId = request.jobId,
                stdout = request.stdout,
                stderr = request.stderr,
                exitCode = request.exitCode,
            )
        )
    }
}

data class RegisterExecutorRequest(
    @field:NotBlank
    val id: String,
    @field:NotBlank
    val podName: String,
    @field:NotBlank
    val namespace: String,
    @field:NotBlank
    val flavor: String,
)

data class ExecutorResponse(
    val id: String,
    val podName: String,
    val namespace: String,
    val flavor: String,
    val status: String,
    val jobId: String?,
    val readyAt: Instant?,
    val lastHeartbeatAt: Instant?,
)

data class ExecutorResultRequest(
    @field:NotBlank
    val jobId: String,
    val stdout: String,
    val stderr: String,
    @field:Min(0)
    val exitCode: Int,
)
