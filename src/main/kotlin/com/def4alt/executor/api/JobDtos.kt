package com.def4alt.executor.api

import com.def4alt.executor.domain.Job
import com.def4alt.executor.domain.ResourceSpec
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class CreateJobRequest(
    @field:NotBlank
    val script: String,
    @field:Valid
    val requiredResources: ResourceSpecRequest,
)

data class ResourceSpecRequest(
    @field:Min(1)
    val cpus: Int,
    @field:Min(1)
    val memory: Int,
) {
    fun toDomain(): ResourceSpec {
        return ResourceSpec(
            cpus = cpus,
            memory = memory,
        )
    }
}

data class JobResponse(
    val id: String,
    val script: String,
    val status: String,
    val requiredResources: ResourceSpecRequest,
    val flavor: String,
    val executorId: String?,
    val stdout: String?,
    val stderr: String?,
    val exitCode: Int?,
    val createdAt: Instant,
    val startedAt: Instant?,
    val finishedAt: Instant?,
) {
    companion object {
        fun from(job: Job): JobResponse {
            return JobResponse(
                id = job.id,
                script = job.script,
                status = job.status.name,
                requiredResources = ResourceSpecRequest(
                    cpus = job.requiredResources.cpus,
                    memory = job.requiredResources.memory,
                ),
                flavor = job.flavor,
                executorId = job.executorId,
                stdout = job.stdout,
                stderr = job.stderr,
                exitCode = job.exitCode,
                createdAt = job.createdAt,
                startedAt = job.startedAt,
                finishedAt = job.finishedAt,
            )
        }
    }
}
