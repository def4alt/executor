package com.def4alt.executor.api

import com.def4alt.executor.application.JobService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/jobs")
class JobController(
    private val jobService: JobService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createJob(@Valid @RequestBody request: CreateJobRequest): JobResponse {
        val job = jobService.createJob(request.script, request.requiredResources.toDomain())

        return JobResponse.from(job)
    }

    @GetMapping("/{id}")
    fun getJob(@PathVariable id: String): JobResponse {
        return JobResponse.from(jobService.getJob(id))
    }
}
