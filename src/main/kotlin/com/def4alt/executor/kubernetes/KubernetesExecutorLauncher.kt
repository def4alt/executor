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
                    .withCommand("sh", "-lc", agentScript())
                    .addNewEnv().withName("EXECUTOR_ID").withValue(executorId).endEnv()
                    .addNewEnv().withName("EXECUTOR_POD_NAME").withValue(podName).endEnv()
                    .addNewEnv().withName("EXECUTOR_NAMESPACE").withValue(namespace).endEnv()
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

    private fun agentScript(): String {
        val dollar = '$'
        return """
        set -eu
        TOKEN_HEADER="X-Executor-Token: ${dollar}EXECUTOR_INTERNAL_AUTH_TOKEN"
        register_payload=${dollar}(printf '{"id":"%s","podName":"%s","namespace":"%s"}' "${dollar}EXECUTOR_ID" "${dollar}EXECUTOR_POD_NAME" "${dollar}EXECUTOR_NAMESPACE")
        wget -qO- \
          --header="Content-Type: application/json" \
          --header="${dollar}TOKEN_HEADER" \
          --post-data="${dollar}register_payload" \
          "${dollar}EXECUTOR_CONTROL_PLANE_URL/internal/executors/register" >/dev/null
        while true; do
          if wget -qO /tmp/assignment.env --header="${dollar}TOKEN_HEADER" "${dollar}EXECUTOR_CONTROL_PLANE_URL/internal/executors/${dollar}EXECUTOR_ID/assignment-script"; then
            if [ -s /tmp/assignment.env ]; then
              . /tmp/assignment.env
              break
            fi
          fi
          sleep 1
        done
        printf '%s' "${dollar}SCRIPT_BASE64" | base64 -d >/tmp/job.sh
        sh /tmp/job.sh >/tmp/stdout 2>/tmp/stderr || EXIT_CODE=${dollar}?
        : "${dollar}{EXIT_CODE:=0}"
        stdout_b64=${dollar}(base64 /tmp/stdout | tr -d '\n')
        stderr_b64=${dollar}(base64 /tmp/stderr | tr -d '\n')
        result_payload=${dollar}(printf '{"jobId":"%s","stdoutBase64":"%s","stderrBase64":"%s","exitCode":%s}' "${dollar}JOB_ID" "${dollar}stdout_b64" "${dollar}stderr_b64" "${dollar}EXIT_CODE")
        wget -qO- \
          --header="Content-Type: application/json" \
          --header="${dollar}TOKEN_HEADER" \
          --post-data="${dollar}result_payload" \
          "${dollar}EXECUTOR_CONTROL_PLANE_URL/internal/executors/${dollar}EXECUTOR_ID/result-base64" >/dev/null
        """.trimIndent()
    }
}
