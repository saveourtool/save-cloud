package org.cqfn.save.backend.utils

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.cqfn.save.currentVersion
import org.cqfn.save.v1_0
import org.cqfn.save.v2_0
import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GroupsConfiguration  {

    @Bean
    fun publicApi(): GroupedOpenApi? {
        return GroupedOpenApi.builder()
            .group("user")
            .pathsToExclude("/api/v2.0/**", "/v2.0/**", "/internal/v2.0/**")
            .pathsToMatch("/api/v1/**", "/v1.0/**", "/internal/v1.0/**")
            .build()
    }

    @Bean
    fun adminApi(): GroupedOpenApi? {
        return GroupedOpenApi.builder()
            .group("admin")
            .pathsToExclude("/api/v1.0/**", "/v1.0/**", "/internal/v1.0/**")
            .pathsToMatch("/api/v2.0/**", "/v2.0/**",  "/internal/v2.0/**")
            .build()
    }

    @Bean
    fun customOpenAPI(): OpenAPI? {
        return OpenAPI()
            .components(Components())
            .info(
                Info()
                    .title("titleI")
                    .version("1.0.0")
            )
    }


}