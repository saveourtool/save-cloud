package org.cqfn.save.backend.utils

import org.cqfn.save.currentVersion
import org.cqfn.save.v1_0
import org.cqfn.save.v2_0
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors.any
import springfox.documentation.builders.PathSelectors.regex
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket

@Configuration
class SwaggerConfiguration {

    @Bean
    fun swaggerAllApi(): Docket? {
        return Docket(DocumentationType.OAS_30)
            .groupName("backend-api")
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(any())
            .build()
            .apiInfo(
                ApiInfoBuilder()
                    .version("All")
                    .title("API")
                    .description("Documentation backend API")
                    .build()
            )
            //.select().build()
    }

    @Bean
    fun swaggerApi10(): Docket? {
        return createDocket(v1_0 + "_")
        //return createDocket(v1_0)
    }

    @Bean
    fun swaggerCurrentApi(): Docket? {
        return createDocket(currentVersion)
    }

    @Bean
    fun swaggerApi20(): Docket? {
        return createDocket(v2_0)
    }

    private fun createDocket(version: String): Docket? {
        return Docket(DocumentationType.OAS_30).groupName("backend-api-${version}")
            .select()
            .apis(RequestHandlerSelectors.basePackage("org.cqfn.save.backend.controllers"))
            .paths(regex("/api/${version}.*"))
            .paths(regex("/internal/${version}.*"))
            .build()
            .apiInfo(
                ApiInfoBuilder()
                    .version(version)
                    .title("API")
                    .description("Documentation backend API ${version}")
                    .build()
            )
    }

}