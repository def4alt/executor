package com.def4alt.executor.api

import com.def4alt.executor.application.ExecutorRepository
import com.def4alt.executor.persistence.JobRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExecutorControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Autowired
    private lateinit var executorRepository: ExecutorRepository

    @Test
    fun `register marks executor ready`() {
        val request = RegisterExecutorRequest(
            id = "exec-1",
            podName = "executor-small-1",
            namespace = "executor-system",
            flavor = "small-linux",
        )

        mockMvc.perform(
            post("/internal/executors/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("exec-1"))
            .andExpect(jsonPath("$.status").value("READY"))
    }

    @Test
    fun `result marks job finished and executor terminated`() {
        val createdJob = objectMapper.readValue(
            mockMvc.perform(
                post("/jobs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(
                            CreateJobRequest(
                                script = "echo done",
                                requiredResources = ResourceSpecRequest(cpus = 1, memory = 512),
                            )
                        )
                    )
            ).andReturn().response.contentAsString,
            JobResponse::class.java,
        )

        mockMvc.perform(
            post("/internal/executors/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        RegisterExecutorRequest(
                            id = "exec-2",
                            podName = "executor-small-2",
                            namespace = "executor-system",
                            flavor = "small-linux",
                        )
                    )
                )
        ).andExpect(status().isCreated)

        executorRepository.attachJob("exec-2", createdJob.id)
        val scheduledJob = requireNotNull(jobRepository.findById(createdJob.id)).copy(
            status = com.def4alt.executor.domain.JobStatus.IN_PROGRESS,
            executorId = "exec-2",
            startedAt = Instant.parse("2026-03-08T10:05:00Z"),
        )
        jobRepository.createOrReplace(scheduledJob)

        mockMvc.perform(
            post("/internal/executors/{id}/result", "exec-2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        ExecutorResultRequest(
                            jobId = createdJob.id,
                            stdout = "done\n",
                            stderr = "",
                            exitCode = 0,
                        )
                    )
                )
        )
            .andExpect(status().isAccepted)

        mockMvc.perform(get("/jobs/{id}", createdJob.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("FINISHED"))
            .andExpect(jsonPath("$.stdout").value("done\n"))
            .andExpect(jsonPath("$.exitCode").value(0))
    }

    @Test
    fun `assignment returns leased job for executor`() {
        val createdJob = objectMapper.readValue(
            mockMvc.perform(
                post("/jobs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(
                            CreateJobRequest(
                                script = "echo assigned",
                                requiredResources = ResourceSpecRequest(cpus = 1, memory = 512),
                            )
                        )
                    )
            ).andReturn().response.contentAsString,
            JobResponse::class.java,
        )

        mockMvc.perform(
            post("/internal/executors/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        RegisterExecutorRequest(
                            id = "exec-3",
                            podName = "executor-small-3",
                            namespace = "executor",
                            flavor = "small-linux",
                        )
                    )
                )
        ).andExpect(status().isCreated)

        executorRepository.attachJob("exec-3", createdJob.id)
        val scheduledJob = requireNotNull(jobRepository.findById(createdJob.id)).copy(
            status = com.def4alt.executor.domain.JobStatus.IN_PROGRESS,
            executorId = "exec-3",
            startedAt = Instant.parse("2026-03-08T10:05:00Z"),
        )
        jobRepository.createOrReplace(scheduledJob)

        mockMvc.perform(get("/internal/executors/{id}/assignment", "exec-3"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.jobId").value(createdJob.id))
            .andExpect(jsonPath("$.script").value("echo assigned"))
    }
}
