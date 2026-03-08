package com.def4alt.executor.kubernetes

import com.def4alt.executor.application.ExecutorLauncher
import com.def4alt.executor.application.ExecutorRuntime
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "executor.kubernetes", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class NoopExecutorLauncher : ExecutorLauncher {
    override fun launch(flavor: String) = Unit
}

@Component
@ConditionalOnProperty(prefix = "executor.kubernetes", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class NoopExecutorRuntime : ExecutorRuntime {
    override fun findTerminatedPods() = emptyList<com.def4alt.executor.application.TerminatedExecutorPod>()
    override fun findFailedPods() = emptyList<com.def4alt.executor.application.FailedExecutorPod>()
    override fun deletePod(podName: String) = Unit
}
