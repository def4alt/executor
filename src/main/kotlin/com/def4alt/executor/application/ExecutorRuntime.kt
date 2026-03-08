package com.def4alt.executor.application

interface ExecutorRuntime {
    fun findTerminatedPods(): List<TerminatedExecutorPod>

    fun findFailedPods(): List<FailedExecutorPod>

    fun deletePod(podName: String)
}

data class TerminatedExecutorPod(
    val executorId: String,
    val podName: String,
)

data class FailedExecutorPod(
    val executorId: String,
    val podName: String,
)
