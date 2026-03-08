package com.def4alt.executor.persistence

import com.def4alt.executor.domain.Job

interface JobRepository {
    fun create(job: Job)

    fun createOrReplace(job: Job)

    fun findById(id: String): Job?
}
