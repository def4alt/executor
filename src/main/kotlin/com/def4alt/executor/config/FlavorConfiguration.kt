package com.def4alt.executor.config

import com.def4alt.executor.ExecutorProperties
import com.def4alt.executor.domain.ExecutorFlavor
import com.def4alt.executor.domain.FlavorCatalog
import com.def4alt.executor.domain.ResourceSpec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlavorConfiguration {
    @Bean
    fun flavorCatalog(properties: ExecutorProperties): FlavorCatalog {
        val flavors = properties.flavors.map {
            ExecutorFlavor(
                name = it.name,
                resources = ResourceSpec(
                    cpus = it.cpus,
                    memory = it.memory,
                ),
            )
        }

        return FlavorCatalog(flavors)
    }
}
