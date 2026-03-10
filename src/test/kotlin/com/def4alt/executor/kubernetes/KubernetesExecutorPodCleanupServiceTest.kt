package com.def4alt.executor.kubernetes

import com.def4alt.executor.ExecutorProperties
import com.def4alt.executor.KubernetesProperties
import com.def4alt.executor.SchedulerProperties
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@EnableKubernetesMockClient(crud = true)
class KubernetesExecutorPodCleanupServiceTest {
    private lateinit var server: KubernetesMockServer
    private lateinit var client: io.fabric8.kubernetes.client.KubernetesClient

    @Test
    fun `cleanupFinishedPods deletes completed executor pods only`() {
        client.pods().inNamespace("executor").resource(executorPod("done-pod", "Succeeded")).create()
        client.pods().inNamespace("executor").resource(executorPod("failed-pod", "Failed")).create()
        client.pods().inNamespace("executor").resource(executorPod("running-pod", "Running")).create()
        client.pods().inNamespace("executor").resource(nonExecutorPod("other-pod", "Succeeded")).create()

        val service = KubernetesExecutorPodCleanupService(client, properties())

        val deletedCount = service.cleanupFinishedPods()

        assertEquals(2, deletedCount)
        assertEquals(listOf("running-pod"), client.pods().inNamespace("executor").withLabel("app", "executor-agent").list().items.map { it.metadata.name })
        assertEquals(listOf("other-pod"), client.pods().inNamespace("executor").withLabel("app", "something-else").list().items.map { it.metadata.name })
    }

    @Test
    fun `cleanupFinishedPods returns zero when there is nothing to delete`() {
        client.pods().inNamespace("executor").resource(executorPod("running-pod", "Running")).create()
        val service = KubernetesExecutorPodCleanupService(client, properties())

        val deletedCount = service.cleanupFinishedPods()

        assertEquals(0, deletedCount)
        assertEquals(listOf("running-pod"), client.pods().inNamespace("executor").withLabel("app", "executor-agent").list().items.map { it.metadata.name })
    }

    private fun properties() = ExecutorProperties(
        scheduler = SchedulerProperties(),
        kubernetes = KubernetesProperties(enabled = true, namespace = "executor"),
    )

    private fun executorPod(name: String, phase: String) = PodBuilder()
        .withNewMetadata()
        .withName(name)
        .withNamespace("executor")
        .addToLabels("app", "executor-agent")
        .endMetadata()
        .withNewStatus()
        .withPhase(phase)
        .endStatus()
        .build()

    private fun nonExecutorPod(name: String, phase: String) = PodBuilder()
        .withNewMetadata()
        .withName(name)
        .withNamespace("executor")
        .addToLabels("app", "something-else")
        .endMetadata()
        .withNewStatus()
        .withPhase(phase)
        .endStatus()
        .build()
}
