package com.def4alt.executor.application

import com.def4alt.executor.ExecutorProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "executor", name = ["mode"], havingValue = "control-plane", matchIfMissing = true)
class ExecutorMaintenanceLoop(
    private val cleanupService: ExecutorPodCleanupService,
    private val properties: ExecutorProperties,
) {
    @Scheduled(fixedDelayString = "\${executor.scheduler.fixed-delay-ms:2000}")
    fun tick() {
        if (!properties.scheduler.enabled) {
            return
        }

        cleanupService.cleanup()
    }
}
