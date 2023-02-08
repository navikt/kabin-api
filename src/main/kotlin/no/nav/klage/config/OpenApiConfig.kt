package no.nav.klage.config

import no.nav.klage.api.controller.CreateAnkeController
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun apiInternal(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .packagesToScan(CreateAnkeController::class.java.packageName)
            .group("internal")
            .pathsToMatch("/**")
            .build()
    }
}
