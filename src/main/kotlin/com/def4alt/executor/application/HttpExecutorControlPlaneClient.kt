package com.def4alt.executor.application

import com.def4alt.executor.ExecutorProperties
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class HttpExecutorControlPlaneClient(
    restClientBuilder: RestClient.Builder,
    properties: ExecutorProperties,
) : ExecutorControlPlaneClient {
    private val restClient = restClientBuilder
        .baseUrl(properties.controlPlaneUrl)
        .defaultHeader("X-Executor-Token", properties.internalAuthToken)
        .build()

    override fun registerExecutor(executorId: String, podName: String, namespace: String) {
        restClient.post()
            .uri("/internal/executors/register")
            .body(mapOf("id" to executorId, "podName" to podName, "namespace" to namespace))
            .retrieve()
            .toBodilessEntity()
    }

    override fun fetchAssignment(executorId: String): ExecutorAssignment? {
        return restClient.get()
            .uri("/internal/executors/{id}/assignment", executorId)
            .exchange { _, response ->
                when (response.statusCode) {
                    HttpStatusCode.valueOf(204) -> null
                    else -> response.bodyTo(ExecutorAssignment::class.java)
                }
            }
    }

    override fun reportResult(executorId: String, command: ExecutorResultCommand) {
        restClient.post()
            .uri("/internal/executors/{id}/result", executorId)
            .body(command)
            .retrieve()
            .toBodilessEntity()
    }
}
