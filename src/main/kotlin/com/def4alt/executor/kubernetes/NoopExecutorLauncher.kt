package com.def4alt.executor.kubernetes

import com.def4alt.executor.application.ExecutorLauncher
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "executor.kubernetes", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class NoopExecutorLauncher : ExecutorLauncher {
    override fun launch(job: com.def4alt.executor.domain.Job): String = "noop-executor"
}
