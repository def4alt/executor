package com.def4alt.executor.application

import com.def4alt.executor.ExecutorProperties
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "executor", name = ["mode"], havingValue = "executor")
class ExecutorModeRunner(
    private val properties: ExecutorProperties,
    private val executorAgentService: ExecutorAgentService,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val command = ExecutorRuntimeCommand(
            executorId = properties.runtime.id,
            podName = properties.runtime.podName,
            namespace = properties.runtime.namespace,
        )

        check(command.executorId.isNotBlank()) { "executor.runtime.id must be set in executor mode" }
        check(command.podName.isNotBlank()) { "executor.runtime.pod-name must be set in executor mode" }
        check(command.namespace.isNotBlank()) { "executor.runtime.namespace must be set in executor mode" }

        val ranJob = executorAgentService.runRegisteredExecutor(command)
        check(ranJob) { "Executor ${command.executorId} did not receive an assigned job" }
    }
}
