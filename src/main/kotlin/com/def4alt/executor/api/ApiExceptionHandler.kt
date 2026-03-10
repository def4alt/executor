package com.def4alt.executor.api

import com.def4alt.executor.application.ExecutorJobOwnershipException
import com.def4alt.executor.application.JobNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(JobNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleJobNotFound(exception: JobNotFoundException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.message ?: "Job not found")
    }

    @ExceptionHandler(ExecutorJobOwnershipException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleExecutorJobOwnership(exception: ExecutorJobOwnershipException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.message ?: "Executor does not own job")
    }
}
