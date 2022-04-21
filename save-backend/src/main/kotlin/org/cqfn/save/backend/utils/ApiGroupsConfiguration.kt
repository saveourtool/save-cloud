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
class ApiGroupsConfiguration {

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
        return createGroupedOpenApi(v1_0, v1_0)
    }

    @Bean
    fun openApiV20(): GroupedOpenApi? {
        return createGroupedOpenApi(v2_0, v2_0)
    }

    @Bean
    fun openApiCurrentVersion(): GroupedOpenApi? {
        return createGroupedOpenApi("latest", currentVersion)
    }

    @Bean
    fun customOpenAPI(): OpenAPI? {
        return OpenAPI()
            .components(Components())
            .info(
                Info()
                    .title("SAVE Backend API")
                    .version(currentVersion)
            )
    }


    // http://localhost:81/swagger-ui/index.html?configUrl=/MyApp/v3/api-docs/swagger-config

    private fun createGroupedOpenApi(groupName: String, version: String): GroupedOpenApi? {
        return GroupedOpenApi.builder()
            .group(groupName)
            .pathsToMatch("/api/${version}/**", "/internal/${version}/**")
            .pathsToExclude("?!(/api/${version}).+", "?!(/internal/${version}).+")
            .packagesToScan("org.cqfn.save.backend.controllers")
            .build()
    }

}