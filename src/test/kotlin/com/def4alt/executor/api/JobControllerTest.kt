package com.def4alt.executor.api

import com.def4alt.executor.persistence.JobRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

    @Test
    fun `post jobs stores a queued job with the requested resources`() {
        val request = createJobRequest(script = "echo hello", cpus = 1, memory = 512)

        val createdJob = createJob(request)

        assertEquals("echo hello", createdJob.script)
        assertEquals("QUEUED", createdJob.status)
        assertEquals(1, createdJob.requiredResources.cpus)
        assertEquals(512, createdJob.requiredResources.memory)
        assertNull(createdJob.executorId)

        val storedJob = jobRepository.findById(createdJob.id)
        assertNotNull(storedJob)
        assertEquals("echo hello", storedJob.script)
        assertEquals(1, storedJob.requiredResources.cpus)
        assertEquals(512, storedJob.requiredResources.memory)
    }

    @Test
    fun `post jobs rejects blank scripts`() {
        val request = createJobRequest(script = "", cpus = 1, memory = 512)

        mockMvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `post jobs rejects cpu values below one`() {
        val request = createJobRequest(script = "echo hello", cpus = 0, memory = 512)

        mockMvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `post jobs rejects memory values below one`() {
        val request = createJobRequest(script = "echo hello", cpus = 1, memory = 0)

        mockMvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `get jobs by id returns the previously created job`() {
        val createdJob = createJob(createJobRequest(script = "echo status", cpus = 1, memory = 512))

        mockMvc.perform(get("/jobs/{id}", createdJob.id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(createdJob.id))
            .andExpect(jsonPath("$.script").value("echo status"))
            .andExpect(jsonPath("$.status").value("QUEUED"))
    }

    @Test
    fun `get jobs by id returns not found for unknown jobs`() {
        mockMvc.perform(get("/jobs/{id}", "missing-job"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.detail").value("Job missing-job not found"))
    }

    private fun createJob(request: CreateJobRequest): JobResponse {
        val body = mockMvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readValue(body, JobResponse::class.java)
    }

    private fun createJobRequest(script: String, cpus: Int, memory: Int): CreateJobRequest {
        return CreateJobRequest(
            script = script,
            requiredResources = ResourceSpecRequest(cpus = cpus, memory = memory),
        )
    }
}
