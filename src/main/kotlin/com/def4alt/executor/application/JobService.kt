package com.def4alt.executor.application

import com.def4alt.executor.domain.FlavorCatalog
import com.def4alt.executor.domain.Job
import com.def4alt.executor.domain.JobStatus
import com.def4alt.executor.domain.ResourceSpec
import com.def4alt.executor.persistence.JobRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class JobService(
    private val flavorCatalog: FlavorCatalog,
    private val jobRepository: JobRepository,
    private val clock: Clock,
) {
    fun createJob(script: String, requiredResources: ResourceSpec): Job {
        val flavor = flavorCatalog.findSmallestMatching(requiredResources)
        val job = Job(
            id = UUID.randomUUID().toString(),
            script = script,
            status = JobStatus.QUEUED,
            requiredResources = requiredResources,
            flavor = flavor.name,
            createdAt = Instant.now(clock),
        )

        jobRepository.create(job)

        return job
    }

    fun getJob(id: String): Job {
        return jobRepository.findById(id) ?: throw JobNotFoundException(id)
    }
}
