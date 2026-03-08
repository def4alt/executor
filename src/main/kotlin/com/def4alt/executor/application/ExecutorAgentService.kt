package com.def4alt.executor.application

class ExecutorAgentService(
    private val controlPlaneClient: ExecutorControlPlaneClient,
    private val shellCommandRunner: ShellCommandRunner,
) {
    fun runSingleAssignedJob(executorId: String): Boolean {
        val assignment = controlPlaneClient.fetchAssignment(executorId) ?: return false
        val result = shellCommandRunner.run(assignment.script)

        controlPlaneClient.reportResult(
            executorId = executorId,
            command = ExecutorResultCommand(
                jobId = assignment.jobId,
                stdout = result.stdout,
                stderr = result.stderr,
                exitCode = result.exitCode,
            )
        )

        return true
    }
}
