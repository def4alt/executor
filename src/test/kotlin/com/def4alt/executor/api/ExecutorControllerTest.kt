package com.def4alt.executor.api

import com.def4alt.executor.application.ExecutorRepository
import com.def4alt.executor.domain.ExecutorStatus
import com.def4alt.executor.domain.JobStatus
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExecutorControllerTest {
    private val internalToken = "test-internal-token"

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
        val response = registerExecutor(
            RegisterExecutorRequest(
                id = "exec-register-1",
                podName = "executor-1",
                namespace = "executor-system",
            )
        )

        assertEquals("exec-register-1", response.id)
        assertEquals("READY", response.status)
        assertNotNull(response.readyAt)

        val storedExecutor = executorRepository.findById("exec-register-1")
        assertNotNull(storedExecutor)
        assertEquals(ExecutorStatus.READY, storedExecutor.status)
        assertNotNull(storedExecutor.readyAt)
    }

    @Test
    fun `register rejects requests without the internal token`() {
        mockMvc.perform(
            post("/internal/executors/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        RegisterExecutorRequest(
                            id = "exec-missing-token",
                            podName = "executor-missing-token",
                            namespace = "executor",
                        )
                    )
                )
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `register is safe to call twice for the same executor`() {
        registerExecutor(
            RegisterExecutorRequest(
                id = "exec-register-2",
                podName = "executor-2",
                namespace = "executor-system",
            )
        )

        val secondResponse = registerExecutor(
            RegisterExecutorRequest(
                id = "exec-register-2",
                podName = "executor-2",
                namespace = "executor-system",
            )
        )

        assertEquals("exec-register-2", secondResponse.id)
        assertEquals("READY", secondResponse.status)

        val storedExecutor = executorRepository.findById("exec-register-2")
        assertNotNull(storedExecutor)
        assertEquals(ExecutorStatus.READY, storedExecutor.status)
    }

    @Test
    fun `assignment returns no content when no job is assigned`() {
        registerExecutor(
            RegisterExecutorRequest(
                id = "exec-assignment-none",
                podName = "executor-3",
                namespace = "executor-system",
            )
        )

        mockMvc.perform(
            get("/internal/executors/{id}/assignment", "exec-assignment-none")
                .header("X-Executor-Token", internalToken)
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `assignment returns the job and marks it in progress on first pickup`() {
        val createdJob = createJob(script = "echo assigned", cpus = 1, memory = 512)
        registerExecutor(
            RegisterExecutorRequest(
                id = "exec-assignment-1",
                podName = "executor-4",
                namespace = "executor",
            )
        )

        val scheduledJob = requireNotNull(jobRepository.findById(createdJob.id)).copy(executorId = "exec-assignment-1")
        jobRepository.createOrReplace(scheduledJob)

        mockMvc.perform(
            get("/internal/executors/{id}/assignment", "exec-assignment-1")
                .header("X-Executor-Token", internalToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.jobId").value(createdJob.id))
            .andExpect(jsonPath("$.script").value("echo assigned"))

        val storedJob = requireNotNull(jobRepository.findById(createdJob.id))
        assertEquals(JobStatus.IN_PROGRESS, storedJob.status)
        assertEquals("exec-assignment-1", storedJob.executorId)
        assertNotNull(storedJob.startedAt)
    }

    @Test
    fun `assignment script returns env payload for busybox executors`() {
        val createdJob = createJob(script = "echo busybox", cpus = 1, memory = 512)
        registerExecutor(
            RegisterExecutorRequest(
                id = "exec-assignment-script-1",
                podName = "executor-script-1",
                namespace = "executor",
            )
        )

        val scheduledJob = requireNotNull(jobRepository.findById(createdJob.id)).copy(executorId = "exec-assignment-script-1")
        jobRepository.createOrReplace(scheduledJob)

        mockMvc.perform(
            get("/internal/executors/{id}/assignment-script", "exec-assignment-script-1")
                .header("X-Executor-Token", internalToken)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("JOB_ID=${createdJob.id}\nSCRIPT_BASE64=${Base64.getEncoder().encodeToString("echo busybox".toByteArray())}\n"))

        val storedJob = requireNotNull(jobRepository.findById(createdJob.id))
        assertEquals(JobStatus.IN_PROGRESS, storedJob.status)
    }

    @Test
    fun `result marks job finished and executor terminated`() {
        val createdJob = createJob(script = "echo done", cpus = 1, memory = 512)
        registerExecutor(
            RegisterExecutorRequest(
                id = "exec-result-1",
                podName = "executor-5",
                namespace = "executor-system",
            )
        )

        val scheduledJob = requireNotNull(jobRepository.findById(createdJob.id)).copy(
            status = JobStatus.IN_PROGRESS,
            executorId = "exec-result-1",
        )
        jobRepository.createOrReplace(scheduledJob)

        mockMvc.perform(
            post("/internal/executors/{id}/result", "exec-result-1")
                .header("X-Executor-Token", internalToken)
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

        val storedJob = requireNotNull(jobRepository.findById(createdJob.id))
        assertEquals(JobStatus.FINISHED, storedJob.status)
        assertEquals("done\n", storedJob.stdout)
        assertEquals(0, storedJob.exitCode)
        assertNotNull(storedJob.finishedAt)

        val storedExecutor = requireNotNull(executorRepository.findById("exec-result-1"))
        assertEquals(ExecutorStatus.TERMINATED, storedExecutor.status)
    }

    @Test
    fun `result marks non-zero exit codes as failed`() {
        val createdJob = createJob(script = "exit 23", cpus = 1, memory = 512)
        registerExecutor(
            RegisterExecutorRequest(
                id = "exec-result-fail",
                podName = "executor-8",
                namespace = "executor-system",
            )
        )

        val scheduledJob = requireNotNull(jobRepository.findById(createdJob.id)).copy(
            status = JobStatus.IN_PROGRESS,
            executorId = "exec-result-fail",
        )
        jobRepository.createOrReplace(scheduledJob)

        mockMvc.perform(
            post("/internal/executors/{id}/result", "exec-result-fail")
                .header("X-Executor-Token", internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        ExecutorResultRequest(
                            jobId = createdJob.id,
                            stdout = "",
                            stderr = "boom",
                            exitCode = 23,
                        )
                    )
                )
        )
            .andExpect(status().isAccepted)

        val storedJob = requireNotNull(jobRepository.findById(createdJob.id))
        assertEquals(JobStatus.FAILED, storedJob.status)
        assertEquals(23, storedJob.exitCode)
        assertEquals("boom", storedJob.stderr)
    }

    @Test
    fun `base64 result decodes payloads from busybox executors`() {
        val createdJob = createJob(script = "printf done", cpus = 1, memory = 512)
        registerExecutor(
            RegisterExecutorRequest(
                id = "exec-result-base64",
                podName = "executor-9",
                namespace = "executor-system",
            )
        )

        val scheduledJob = requireNotNull(jobRepository.findById(createdJob.id)).copy(
            status = JobStatus.IN_PROGRESS,
            executorId = "exec-result-base64",
        )
        jobRepository.createOrReplace(scheduledJob)

        mockMvc.perform(
            post("/internal/executors/{id}/result-base64", "exec-result-base64")
                .header("X-Executor-Token", internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        ExecutorBase64ResultRequest(
                            jobId = createdJob.id,
                            stdoutBase64 = Base64.getEncoder().encodeToString("done".toByteArray()),
                            stderrBase64 = Base64.getEncoder().encodeToString("".toByteArray()),
                            exitCode = 0,
                        )
                    )
                )
        )
            .andExpect(status().isAccepted)

        val storedJob = requireNotNull(jobRepository.findById(createdJob.id))
        assertEquals(JobStatus.FINISHED, storedJob.status)
        assertEquals("done", storedJob.stdout)
    }

    @Test
    fun `result rejects executors that do not own the job`() {
        val createdJob = createJob(script = "echo nope", cpus = 1, memory = 512)
        registerExecutor(RegisterExecutorRequest(id = "exec-owner", podName = "executor-6", namespace = "executor"))
        registerExecutor(RegisterExecutorRequest(id = "exec-stranger", podName = "executor-7", namespace = "executor"))

        val scheduledJob = requireNotNull(jobRepository.findById(createdJob.id)).copy(
            status = JobStatus.IN_PROGRESS,
            executorId = "exec-owner",
        )
        jobRepository.createOrReplace(scheduledJob)

        mockMvc.perform(
            post("/internal/executors/{id}/result", "exec-stranger")
                .header("X-Executor-Token", internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        ExecutorResultRequest(
                            jobId = createdJob.id,
                            stdout = "",
                            stderr = "",
                            exitCode = 0,
                        )
                    )
                )
        )
            .andExpect(status().isConflict)

        val storedJob = requireNotNull(jobRepository.findById(createdJob.id))
        assertEquals(JobStatus.IN_PROGRESS, storedJob.status)
        assertEquals("exec-owner", storedJob.executorId)

        val strangerExecutor = requireNotNull(executorRepository.findById("exec-stranger"))
        assertEquals(ExecutorStatus.READY, strangerExecutor.status)
    }

    private fun registerExecutor(request: RegisterExecutorRequest): ExecutorResponse {
        val body = mockMvc.perform(
            post("/internal/executors/register")
                .header("X-Executor-Token", internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readValue(body, ExecutorResponse::class.java)
    }

    private fun createJob(script: String, cpus: Int, memory: Int): JobResponse {
        val body = mockMvc.perform(
            post("/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        CreateJobRequest(
                            script = script,
                            requiredResources = ResourceSpecRequest(cpus = cpus, memory = memory),
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readValue(body, JobResponse::class.java)
    }
}
