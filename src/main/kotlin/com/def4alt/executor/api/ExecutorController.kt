package com.def4alt.executor.api

import com.def4alt.executor.application.ExecutorResultCommand
import com.def4alt.executor.application.ExecutorResultService
import com.def4alt.executor.application.ExecutorAssignmentService
import com.def4alt.executor.application.ExecutorService
import com.def4alt.executor.application.RegisterExecutorCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.GetMapping
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
    private val executorAssignmentService: ExecutorAssignmentService,
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

    @PostMapping("/{id}/result-base64")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun recordBase64Result(@PathVariable id: String, @Valid @RequestBody request: ExecutorBase64ResultRequest) {
        executorResultService.recordResult(
            executorId = id,
            request = ExecutorResultCommand(
                jobId = request.jobId,
                stdout = request.decodeStdout(),
                stderr = request.decodeStderr(),
                exitCode = request.exitCode,
            )
        )
    }

    @GetMapping("/{id}/assignment")
    fun getAssignment(@PathVariable id: String): ResponseEntity<ExecutorAssignmentResponse> {
        val assignment = executorAssignmentService.getAssignment(id)
            ?: return ResponseEntity.noContent().build()

        return ResponseEntity.ok(
            ExecutorAssignmentResponse(
                jobId = assignment.jobId,
                script = assignment.script,
            )
        )
    }

    @GetMapping("/{id}/assignment-script", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getAssignmentScript(@PathVariable id: String): ResponseEntity<String> {
        val assignmentScript = executorAssignmentService.getAssignmentScript(id)
            ?: return ResponseEntity.noContent().build()

        return ResponseEntity.ok(assignmentScript)
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

data class ExecutorBase64ResultRequest(
    @field:NotBlank
    val jobId: String,
    val stdoutBase64: String,
    val stderrBase64: String,
    @field:Min(0)
    val exitCode: Int,
) {
    fun decodeStdout(): String = java.util.Base64.getDecoder().decode(stdoutBase64).decodeToString()

    fun decodeStderr(): String = java.util.Base64.getDecoder().decode(stderrBase64).decodeToString()
}

data class ExecutorAssignmentResponse(
    val jobId: String,
    val script: String,
)
