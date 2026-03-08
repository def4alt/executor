package com.def4alt.executor.persistence

import com.def4alt.executor.domain.Job
import com.def4alt.executor.domain.JobStatus

interface JobRepository {
    fun create(job: Job)

    fun createOrReplace(job: Job)

    fun findById(id: String): Job?

    fun countByStatus(status: JobStatus): Int
}
