package com.def4alt.executor

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "executor")
data class ExecutorProperties(
    val mode: String = "control-plane",
    val controlPlaneUrl: String = "http://executor:8080",
    val internalAuthToken: String = "change-me",
    val scheduler: SchedulerProperties = SchedulerProperties(),
    val kubernetes: KubernetesProperties = KubernetesProperties(),
    val runtime: ExecutorRuntimeProperties = ExecutorRuntimeProperties(),
)

data class SchedulerProperties(
    val enabled: Boolean = true,
    val fixedDelayMs: Long = 2_000,
)

data class KubernetesProperties(
    val enabled: Boolean = true,
    val namespace: String = "executor",
    val controlPlaneServiceUrl: String = "http://executor:8080",
    val executorImage: String = "ghcr.io/def4alt/executor:latest",
    val imagePullPolicy: String = "IfNotPresent",
)

data class ExecutorRuntimeProperties(
    val id: String = "",
    val podName: String = "",
    val namespace: String = "",
)
