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
    fun openApiAll(): GroupedOpenApi? {
        return GroupedOpenApi.builder()
            .group("all")
            .pathsToMatch("/api/**", "/internal/**")
            .packagesToScan("org.cqfn.save.backend.controllers")
            .build()
    }

    @Bean
    fun openApiV10(): GroupedOpenApi? {
        return createGroupedOpenApi(v1_0 + "_")
    }

    @Bean
    fun openApiV20(): GroupedOpenApi? {
        return createGroupedOpenApi(v2_0)
    }

    @Bean
    fun openApiCurrentVersion(): GroupedOpenApi? {
        return createGroupedOpenApi(currentVersion)
    }

    @Bean
    fun customOpenAPI(): OpenAPI? {
        return OpenAPI()
            .components(Components())
            .info(
                Info()
                    .title("SAVE Backend API")
                    .version("1.0.0")
            )
    }


    // http://localhost:81/swagger-ui/index.html?configUrl=/MyApp/v3/api-docs/swagger-config

    private fun createGroupedOpenApi(version: String): GroupedOpenApi? {
        return GroupedOpenApi.builder()
            .group(version)
            .pathsToMatch("/api/${version}/**", "/internal/${version}/**")
            .pathsToExclude("?!(/api/${version}).+", "?!(/internal/${version}).+")
            .packagesToScan("org.cqfn.save.backend.controllers")
            .build()
    }

}