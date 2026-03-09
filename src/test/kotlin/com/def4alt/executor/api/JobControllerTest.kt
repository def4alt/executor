package com.def4alt.executor.api

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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
}
