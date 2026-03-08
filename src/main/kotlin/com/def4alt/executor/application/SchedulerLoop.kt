package com.def4alt.executor.application

import com.def4alt.executor.ExecutorProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
@ConditionalOnProperty(prefix = "executor", name = ["mode"], havingValue = "control-plane", matchIfMissing = true)
class SchedulerLoop(
    private val schedulerService: SchedulerService,
    private val properties: ExecutorProperties,
    private val clock: Clock,
) {
    @Scheduled(fixedDelayString = "\${executor.scheduler.fixed-delay-ms:2000}")
    fun tick() {
        if (!properties.scheduler.enabled) {
            return
        }

        schedulerService.scheduleNextQueuedJob(Instant.now(clock))
    }
}

@Configuration
class SchedulerConfiguration {
    @Bean
    fun schedulerService(
        jobRepository: SchedulingJobRepository,
        executorRepository: ExecutorRepository,
        executorLauncher: ExecutorLauncher,
        properties: ExecutorProperties,
    ): SchedulerService {
        return SchedulerService(
            jobRepository = jobRepository,
            executorRepository = executorRepository,
            executorLauncher = executorLauncher,
        )
    }
}
