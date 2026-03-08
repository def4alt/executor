package com.def4alt.executor.api

import com.def4alt.executor.application.JobSummary
import com.def4alt.executor.application.JobSummaryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin")
class AdminController(
    private val jobSummaryService: JobSummaryService,
) {
    @GetMapping("/summary")
    fun getSummary(): JobSummary {
        return jobSummaryService.getSummary()
    }
}
