package org.cqfn.save.backend.utils

import org.cqfn.save.latestVersion
import org.cqfn.save.v1

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for OpenAPI, which is responsible for creation of tabs,
 * which are grouping the endpoints by versions
 */
@Configuration
class ApiGroupsConfiguration {
    @Bean
    fun openApiAll(): GroupedOpenApi? = GroupedOpenApi.builder()
        .group("all")
        .pathsToMatch("/api/**")
        .packagesToScan("org.cqfn.save.backend.controllers")
        .build()

    @Bean
    fun openApiV1(): GroupedOpenApi? = createGroupedOpenApi(v1, v1)

    @Bean
    fun openApiCurrentVersion(): GroupedOpenApi? = createGroupedOpenApi("latest", latestVersion)

    @Bean
    fun customOpenApi(): OpenAPI? = OpenAPI()
        .components(Components())
        .info(
            Info()
                .title("SAVE Backend API")
                .version(latestVersion)
        )

    private fun createGroupedOpenApi(groupName: String, version: String): GroupedOpenApi? = GroupedOpenApi.builder()
        .group(groupName)
        .pathsToMatch("/api/$version/**")
        .pathsToExclude("?!(/api/$version).+")
        .packagesToScan("org.cqfn.save.backend.controllers")
        .build()
}
