package com.def4alt.executor.config

import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KubernetesClientConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "executor.kubernetes", name = ["enabled"], havingValue = "true")
    fun kubernetesClient(): KubernetesClient {
        return KubernetesClientBuilder().build()
    }
}
