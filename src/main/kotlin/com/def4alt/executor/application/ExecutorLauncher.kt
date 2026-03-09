package com.def4alt.executor.application

import com.def4alt.executor.domain.Job

interface ExecutorLauncher {
    fun launch(job: Job): String
}
