package com.def4alt.executor.application

import com.def4alt.executor.domain.JobStatus
import com.def4alt.executor.persistence.JobRepository
import org.springframework.stereotype.Service

@Service
class JobSummaryService(
    private val jobRepository: JobRepository,
) {
    fun getSummary(): JobSummary {
        return JobSummary(
            queued = jobRepository.countByStatus(JobStatus.QUEUED),
            running = jobRepository.countByStatus(JobStatus.IN_PROGRESS),
            finished = jobRepository.countByStatus(JobStatus.FINISHED),
            failed = jobRepository.countByStatus(JobStatus.FAILED),
        )
    }
}

data class JobSummary(
    val queued: Int,
    val running: Int,
    val finished: Int,
    val failed: Int,
)
