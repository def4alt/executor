package com.def4alt.executor.kubernetes

import com.def4alt.executor.ExecutorProperties
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "executor.kubernetes", name = ["enabled"], havingValue = "true")
class KubernetesExecutorPodCleanupService(
    private val kubernetesClient: KubernetesClient,
    private val properties: ExecutorProperties,
) {
    fun cleanupFinishedPods(): Int {
        val finishedPods = kubernetesClient.pods()
            .inNamespace(properties.kubernetes.namespace)
            .withLabel("app", "executor-agent")
            .list()
            .items
            .filter { it.status?.phase in setOf("Succeeded", "Failed") }

        finishedPods.forEach { pod ->
            kubernetesClient.pods()
                .inNamespace(properties.kubernetes.namespace)
                .withName(pod.metadata.name)
                .delete()
        }

        return finishedPods.size
    }
}

@Component
@ConditionalOnProperty(prefix = "executor.kubernetes", name = ["enabled"], havingValue = "true")
class KubernetesExecutorPodCleanupLoop(
    private val cleanupService: KubernetesExecutorPodCleanupService,
) {
    @Scheduled(fixedDelayString = "\${executor.kubernetes.cleanup-fixed-delay-ms:30000}")
    fun tick() {
        cleanupService.cleanupFinishedPods()
    }
}
