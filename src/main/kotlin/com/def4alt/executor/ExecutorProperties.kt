package com.def4alt.executor

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "executor")
data class ExecutorProperties(
    val flavors: List<FlavorProperties> = emptyList(),
)

data class FlavorProperties(
    val name: String,
    val cpus: Int,
    val memory: Int,
)
