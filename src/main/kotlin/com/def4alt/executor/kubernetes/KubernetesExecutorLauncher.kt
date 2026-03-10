package com.def4alt.executor.kubernetes

import com.def4alt.executor.ExecutorProperties
import com.def4alt.executor.application.ExecutorLauncher
import com.def4alt.executor.application.ExecutorRepository
import com.def4alt.executor.domain.Executor
import com.def4alt.executor.domain.ExecutorStatus
import com.def4alt.executor.domain.Job
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
@ConditionalOnProperty(prefix = "executor.kubernetes", name = ["enabled"], havingValue = "true")
class KubernetesExecutorLauncher(
    private val kubernetesClient: KubernetesClient,
    private val executorRepository: ExecutorRepository,
    private val properties: ExecutorProperties,
    private val clock: Clock,
) : ExecutorLauncher {
    override fun launch(executorId: String, job: Job) {
        val podName = "executor-${executorId.take(8)}"
        val namespace = properties.kubernetes.namespace
        val createdAt = Instant.now(clock)

        executorRepository.create(
            Executor(
                id = executorId,
                podName = podName,
                namespace = namespace,
                status = ExecutorStatus.STARTING,
                jobId = job.id,
                createdAt = createdAt,
            )
        )

        try {
            kubernetesClient.pods().inNamespace(namespace).resource(
                PodBuilder()
                    .withNewMetadata()
                    .withName(podName)
                    .addToLabels("app", "executor-agent")
                    .addToLabels("executor.id", executorId)
                    .endMetadata()
                    .withNewSpec()
                    .withRestartPolicy("Never")
                    .addNewContainer()
                    .withName("executor-agent")
                    .withImage(properties.kubernetes.executorImage)
                    .withImagePullPolicy(properties.kubernetes.imagePullPolicy)
                    .withArgs("--spring.main.web-application-type=none")
                    .addNewEnv().withName("EXECUTOR_MODE").withValue("executor").endEnv()
                    .addNewEnv().withName("EXECUTOR_RUNTIME_ID").withValue(executorId).endEnv()
                    .addNewEnv().withName("EXECUTOR_RUNTIME_POD_NAME").withValue(podName).endEnv()
                    .addNewEnv().withName("EXECUTOR_RUNTIME_NAMESPACE").withValue(namespace).endEnv()
                    .addNewEnv().withName("EXECUTOR_CONTROL_PLANE_URL").withValue(properties.kubernetes.controlPlaneServiceUrl).endEnv()
                    .addNewEnv().withName("EXECUTOR_INTERNAL_AUTH_TOKEN").withValue(properties.internalAuthToken).endEnv()
                    .withResources(
                        ResourceRequirementsBuilder()
                            .addToRequests("cpu", Quantity(job.requiredResources.cpus.toString()))
                            .addToRequests("memory", Quantity("${job.requiredResources.memory}Mi"))
                            .addToLimits("cpu", Quantity(job.requiredResources.cpus.toString()))
                            .addToLimits("memory", Quantity("${job.requiredResources.memory}Mi"))
                            .build()
                    )
                    .endContainer()
                    .endSpec()
                    .build()
            ).create()
        } catch (exception: Exception) {
            executorRepository.markTerminated(executorId)
            throw exception
        }
    }
}
