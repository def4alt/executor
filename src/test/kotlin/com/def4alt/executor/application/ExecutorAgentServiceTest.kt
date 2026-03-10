package com.def4alt.executor.application

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExecutorAgentServiceTest {
    @Test
    fun `runSingleAssignedJob runs the assigned script and reports a successful result`() {
        val client = FakeExecutorControlPlaneClient(
            assignment = ExecutorAssignment(jobId = "job-1", script = "echo hello")
        )
        val runner = FakeShellCommandRunner(
            result = ShellCommandResult(stdout = "hello\n", stderr = "", exitCode = 0)
        )
        val service = ExecutorAgentService(client, runner)

        val ranJob = service.runSingleAssignedJob("exec-1")

        assertTrue(ranJob)
        assertEquals(listOf("exec-1"), client.assignmentLookups)
        assertEquals(listOf("echo hello"), runner.commands)
        assertEquals(
            listOf(
                ExecutorResultCommand(
                    jobId = "job-1",
                    stdout = "hello\n",
                    stderr = "",
                    exitCode = 0,
                )
            ),
            client.reportedResults,
        )
    }

    @Test
    fun `runSingleAssignedJob reports stderr and non-zero exit codes`() {
        val script = "printf 'boom' >&2; exit 23"
        val client = FakeExecutorControlPlaneClient(
            assignment = ExecutorAssignment(jobId = "job-2", script = script)
        )
        val runner = FakeShellCommandRunner(
            result = ShellCommandResult(stdout = "", stderr = "boom", exitCode = 23)
        )
        val service = ExecutorAgentService(client, runner)

        val ranJob = service.runSingleAssignedJob("exec-2")

        assertTrue(ranJob)
        assertEquals(listOf(script), runner.commands)
        assertEquals(
            listOf(
                ExecutorResultCommand(
                    jobId = "job-2",
                    stdout = "",
                    stderr = "boom",
                    exitCode = 23,
                )
            ),
            client.reportedResults,
        )
    }

    @Test
    fun `runSingleAssignedJob preserves multi-line scripts exactly`() {
        val script = "echo first\necho second"
        val client = FakeExecutorControlPlaneClient(
            assignment = ExecutorAssignment(jobId = "job-3", script = script)
        )
        val runner = FakeShellCommandRunner(
            result = ShellCommandResult(stdout = "first\nsecond\n", stderr = "", exitCode = 0)
        )
        val service = ExecutorAgentService(client, runner)

        val ranJob = service.runSingleAssignedJob("exec-3")

        assertTrue(ranJob)
        assertEquals(listOf(script), runner.commands)
        assertEquals(1, client.assignmentLookups.size)
        assertEquals(1, client.reportedResults.size)
    }

    @Test
    fun `runSingleAssignedJob does nothing when no assignment is available`() {
        val client = FakeExecutorControlPlaneClient(assignment = null)
        val runner = FakeShellCommandRunner(
            result = ShellCommandResult(stdout = "", stderr = "", exitCode = 0)
        )
        val service = ExecutorAgentService(client, runner)

        val ranJob = service.runSingleAssignedJob("exec-4")

        assertFalse(ranJob)
        assertEquals(listOf("exec-4"), client.assignmentLookups)
        assertTrue(runner.commands.isEmpty())
        assertTrue(client.reportedResults.isEmpty())
    }
}

private class FakeExecutorControlPlaneClient(
    private val assignment: ExecutorAssignment?,
) : ExecutorControlPlaneClient {
    val assignmentLookups = mutableListOf<String>()
    val reportedResults = mutableListOf<ExecutorResultCommand>()

    override fun registerExecutor(executorId: String, podName: String, namespace: String) = Unit

    override fun fetchAssignment(executorId: String): ExecutorAssignment? {
        assignmentLookups += executorId
        return assignment
    }

    override fun reportResult(executorId: String, command: ExecutorResultCommand) {
        reportedResults += command
    }
}

private class FakeShellCommandRunner(
    private val result: ShellCommandResult,
) : ShellCommandRunner {
    val commands = mutableListOf<String>()

    override fun run(script: String): ShellCommandResult {
        commands += script
        return result
    }
}
