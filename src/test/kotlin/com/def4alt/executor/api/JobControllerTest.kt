package com.def4alt.executor.api

import com.def4alt.executor.domain.JobStatus
import com.def4alt.executor.persistence.JobRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    @Test
    fun `post jobs stores a queued job and returns the created record`() {
        val request = CreateJobRequest(
            script = "echo hello",
            requiredResources = ResourceSpecRequest(cpus = 1, memory = 512),
        )

        mockMvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.flavor").value("small-linux"))
            .andExpect(jsonPath("$.script").value("echo hello"))
    }

    @Test
    fun `get jobs by id returns the previously created job`() {
        val request = CreateJobRequest(
            script = "echo status",
            requiredResources = ResourceSpecRequest(cpus = 1, memory = 512),
        )

        val body = mockMvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val createdJob = objectMapper.readValue(body, JobResponse::class.java)

        mockMvc.perform(get("/jobs/{id}", createdJob.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(createdJob.id))
            .andExpect(jsonPath("$.script").value("echo status"))
            .andExpect(jsonPath("$.status").value("QUEUED"))
    }

    @Test
    fun `admin summary returns queued running finished and failed job counts`() {
        jdbcClient.sql("delete from jobs").update()

        val queuedJob = objectMapper.readValue(
            mockMvc.perform(
                post("/jobs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(
                            CreateJobRequest(
                                script = "echo queued",
                                requiredResources = ResourceSpecRequest(cpus = 1, memory = 128),
                            )
                        )
                    )
            ).andReturn().response.contentAsString,
            JobResponse::class.java,
        )

        val runningJob = queuedJob.copy(id = "job-running", status = JobStatus.IN_PROGRESS.name, startedAt = Instant.parse("2026-03-08T10:00:00Z"))
        val finishedJob = queuedJob.copy(id = "job-finished", status = JobStatus.FINISHED.name, finishedAt = Instant.parse("2026-03-08T10:05:00Z"), stdout = "done\n", exitCode = 0)
        val failedJob = queuedJob.copy(id = "job-failed", status = JobStatus.FAILED.name, finishedAt = Instant.parse("2026-03-08T10:06:00Z"), stderr = "boom")

        jobRepository.createOrReplace(runningJob.toDomain())
        jobRepository.createOrReplace(finishedJob.toDomain())
        jobRepository.createOrReplace(failedJob.toDomain())

        mockMvc.perform(get("/admin/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.queued").value(1))
            .andExpect(jsonPath("$.running").value(1))
            .andExpect(jsonPath("$.finished").value(1))
            .andExpect(jsonPath("$.failed").value(1))
    }

    private fun JobResponse.toDomain() = com.def4alt.executor.domain.Job(
        id = id,
        script = script,
        status = JobStatus.valueOf(status),
        requiredResources = com.def4alt.executor.domain.ResourceSpec(
            cpus = requiredResources.cpus,
            memory = requiredResources.memory,
        ),
        flavor = flavor,
        executorId = executorId,
        stdout = stdout,
        stderr = stderr,
        exitCode = exitCode,
        createdAt = createdAt,
        startedAt = startedAt,
        finishedAt = finishedAt,
    )
}
