package com.def4alt.executor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ExecutorProperties::class)
class ExecutorApplication

fun main(args: Array<String>) {
    runApplication<ExecutorApplication>(*args)
}
