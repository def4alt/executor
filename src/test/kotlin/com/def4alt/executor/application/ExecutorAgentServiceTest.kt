package com.def4alt.executor.application

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExecutorAgentServiceTest {
    @Test
    fun `runSingleAssignedJob runs the script and reports the result`() {
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
    fun `runSingleAssignedJob does nothing when no assignment is available`() {
        val client = FakeExecutorControlPlaneClient(assignment = null)
        val runner = FakeShellCommandRunner(
            result = ShellCommandResult(stdout = "", stderr = "", exitCode = 0)
        )
        val service = ExecutorAgentService(client, runner)

        val ranJob = service.runSingleAssignedJob("exec-1")

        assertFalse(ranJob)
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
