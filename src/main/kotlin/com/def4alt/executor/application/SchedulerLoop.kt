package com.def4alt.executor.application

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnExpression("\${executor.scheduler.enabled:true}")
class SchedulerLoop(
    private val schedulerService: SchedulerService,
) {
    @Scheduled(fixedDelayString = "\${executor.scheduler.fixed-delay-ms:2000}")
    fun tick() {
        schedulerService.scheduleNextQueuedJob()
    }
}

@Configuration
class SchedulerConfiguration {
    @Bean
    @ConditionalOnExpression("\${executor.scheduler.enabled:true}")
    fun schedulerService(
        jobRepository: SchedulingJobRepository,
        executorLauncher: ExecutorLauncher,
    ): SchedulerService {
        return SchedulerService(
            jobRepository = jobRepository,
            executorLauncher = executorLauncher,
        )
    }
}
