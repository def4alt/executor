package com.def4alt.executor.kubernetes

import com.def4alt.executor.ExecutorProperties
import com.def4alt.executor.application.ExecutorRuntime
import com.def4alt.executor.application.FailedExecutorPod
import com.def4alt.executor.application.TerminatedExecutorPod
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "executor.kubernetes", name = ["enabled"], havingValue = "true")
class KubernetesExecutorRuntime(
    private val kubernetesClient: KubernetesClient,
    private val properties: ExecutorProperties,
) : ExecutorRuntime {
    override fun findTerminatedPods(): List<TerminatedExecutorPod> {
        return kubernetesClient.pods()
            .inNamespace(properties.kubernetes.namespace)
            .withLabel("app", "executor-agent")
            .list()
            .items
            .filter { it.status?.phase == "Succeeded" }
            .mapNotNull {
                val executorId = it.metadata?.labels?.get("executor.id") ?: return@mapNotNull null
                val podName = it.metadata?.name ?: return@mapNotNull null
                TerminatedExecutorPod(executorId = executorId, podName = podName)
            }
    }

    override fun findFailedPods(): List<FailedExecutorPod> {
        return kubernetesClient.pods()
            .inNamespace(properties.kubernetes.namespace)
            .withLabel("app", "executor-agent")
            .list()
            .items
            .filter { it.status?.phase == "Failed" }
            .mapNotNull {
                val executorId = it.metadata?.labels?.get("executor.id") ?: return@mapNotNull null
                val podName = it.metadata?.name ?: return@mapNotNull null
                FailedExecutorPod(executorId = executorId, podName = podName)
            }
    }

    override fun deletePod(podName: String) {
        kubernetesClient.pods().inNamespace(properties.kubernetes.namespace).withName(podName).delete()
    }
}
