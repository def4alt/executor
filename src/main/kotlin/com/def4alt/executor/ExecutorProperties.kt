package com.def4alt.executor

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "executor")
data class ExecutorProperties(
    val internalAuthToken: String = "change-me",
    val scheduler: SchedulerProperties = SchedulerProperties(),
    val kubernetes: KubernetesProperties = KubernetesProperties(),
)

data class SchedulerProperties(
    val enabled: Boolean = true,
    val fixedDelayMs: Long = 2_000,
)

data class KubernetesProperties(
    val enabled: Boolean = true,
    val namespace: String = "executor",
    val controlPlaneServiceUrl: String = "http://executor:8080",
    val executorImage: String = "busybox:latest",
    val imagePullPolicy: String = "IfNotPresent",
)
