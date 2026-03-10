package com.def4alt.executor.application

class ExecutorJobOwnershipException(
    executorId: String,
    jobId: String,
) : RuntimeException("Executor $executorId does not own job $jobId")
