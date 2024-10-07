package no.nav.klage.config

import no.nav.klage.api.controller.CommonController
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun apiInternal(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .packagesToScan(CommonController::class.java.packageName)
            .group("internal")
            .pathsToMatch("/**")
            .build()
    }
}
