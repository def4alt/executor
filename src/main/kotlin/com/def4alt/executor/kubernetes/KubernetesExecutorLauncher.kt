package com.def4alt.executor.kubernetes

import com.def4alt.executor.ExecutorProperties
import com.def4alt.executor.application.ExecutorLauncher
import com.def4alt.executor.application.ExecutorRepository
import com.def4alt.executor.domain.Executor
import com.def4alt.executor.domain.ExecutorStatus
import com.def4alt.executor.domain.FlavorCatalog
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Component
@ConditionalOnProperty(prefix = "executor.kubernetes", name = ["enabled"], havingValue = "true")
class KubernetesExecutorLauncher(
    private val kubernetesClient: KubernetesClient,
    private val executorRepository: ExecutorRepository,
    private val flavorCatalog: FlavorCatalog,
    private val properties: ExecutorProperties,
    private val clock: Clock,
) : ExecutorLauncher {
    override fun launch(flavor: String) {
        val executorId = UUID.randomUUID().toString()
        val podName = "executor-${flavor.take(12)}-${executorId.take(8)}"
        val namespace = properties.kubernetes.namespace
        val executorFlavor = flavorCatalog.getByName(flavor)
        val createdAt = Instant.now(clock)

        executorRepository.create(
            Executor(
                id = executorId,
                podName = podName,
                namespace = namespace,
                flavor = flavor,
                status = ExecutorStatus.STARTING,
                createdAt = createdAt,
            )
        )

        try {
            kubernetesClient.pods().inNamespace(namespace).resource(
                PodBuilder()
                    .withNewMetadata()
                    .withName(podName)
                    .addToLabels("app", "executor-agent")
                    .addToLabels("executor.flavor", flavor)
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
                    .addNewEnv().withName("EXECUTOR_FLAVOR").withValue(flavor).endEnv()
                    .addNewEnv().withName("EXECUTOR_POD_NAME").withValue(podName).endEnv()
                    .addNewEnv().withName("EXECUTOR_NAMESPACE").withValue(namespace).endEnv()
                    .addNewEnv().withName("EXECUTOR_CONTROL_PLANE_URL").withValue(properties.kubernetes.controlPlaneServiceUrl).endEnv()
                    .withResources(
                        ResourceRequirementsBuilder()
                            .addToRequests("cpu", Quantity(executorFlavor.resources.cpus.toString()))
                            .addToRequests("memory", Quantity("${executorFlavor.resources.memory}Mi"))
                            .addToLimits("cpu", Quantity(executorFlavor.resources.cpus.toString()))
                            .addToLimits("memory", Quantity("${executorFlavor.resources.memory}Mi"))
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
        echo "registering executor ${dollar}EXECUTOR_ID"
        register_payload=${dollar}(printf '{"id":"%s","podName":"%s","namespace":"%s","flavor":"%s"}' "${dollar}EXECUTOR_ID" "${dollar}EXECUTOR_POD_NAME" "${dollar}EXECUTOR_NAMESPACE" "${dollar}EXECUTOR_FLAVOR")
        curl -fsS -X POST -H 'Content-Type: application/json' -d "${dollar}register_payload" "${dollar}EXECUTOR_CONTROL_PLANE_URL/internal/executors/register" >/dev/null
        while true; do
          status=${dollar}(curl -sS -o /tmp/assignment.env -w '%{http_code}' "${dollar}EXECUTOR_CONTROL_PLANE_URL/internal/executors/${dollar}EXECUTOR_ID/assignment-script")
          if [ "${dollar}status" = "204" ]; then
            sleep 1
            continue
          fi
          if [ "${dollar}status" = "200" ]; then
            . /tmp/assignment.env
            break
          fi
          echo "assignment poll failed with status ${dollar}status" >&2
          exit 1
        done
        echo "running job ${dollar}JOB_ID"
        printf '%s' "${dollar}SCRIPT_BASE64" | base64 -d >/tmp/job.sh
        sh /tmp/job.sh >/tmp/stdout 2>/tmp/stderr || EXIT_CODE=${dollar}?
        : "${dollar}{EXIT_CODE:=0}"
        stdout_b64=${dollar}(base64 </tmp/stdout | tr -d '\n')
        stderr_b64=${dollar}(base64 </tmp/stderr | tr -d '\n')
        result_payload=${dollar}(printf '{"jobId":"%s","stdoutBase64":"%s","stderrBase64":"%s","exitCode":%s}' "${dollar}JOB_ID" "${dollar}stdout_b64" "${dollar}stderr_b64" "${dollar}EXIT_CODE")
        result_status=${dollar}(curl -sS -o /tmp/result-response -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d "${dollar}result_payload" "${dollar}EXECUTOR_CONTROL_PLANE_URL/internal/executors/${dollar}EXECUTOR_ID/result-base64")
        if [ "${dollar}result_status" != "202" ]; then
          echo "result callback failed with status ${dollar}result_status" >&2
          cat /tmp/result-response >&2 || true
          exit 1
        fi
        echo "finished job ${dollar}JOB_ID"
        """.trimIndent()
    }
}
