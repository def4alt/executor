package com.def4alt.executor.application

interface ExecutorControlPlaneClient {
    fun registerExecutor(executorId: String, podName: String, namespace: String, flavor: String)

    fun fetchAssignment(executorId: String): ExecutorAssignment?

    fun reportResult(executorId: String, command: ExecutorResultCommand)
}
