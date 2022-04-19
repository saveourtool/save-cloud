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
import springfox.documentation.swagger2.annotations.EnableSwagger2

@EnableSwagger2
@Configuration
class SwaggerConfiguration {

    @Bean
    fun swaggerAllApi(): Docket? {
        return Docket(DocumentationType.OAS_30)
            .groupName("save-backend-api-all")
            .select()
            .apis(RequestHandlerSelectors.basePackage("org.cqfn.save.backend.controllers.*"))
            .paths(any())
            .build()
            .apiInfo(
                ApiInfoBuilder()
                    .version("All")
                    .title("API")
                    .description("Documentation backend API")
                    .build()
            )
    }

    @Bean
    fun swaggerApi10(): Docket? {
        return Docket(DocumentationType.OAS_30)
            .groupName("backend-api-${v1_0}")
            .select()
            .apis(RequestHandlerSelectors.basePackage("org.cqfn.save.backend.controllers.*"))
            .paths(regex("\\*/${v1_0}\\*"))
            .build()
            .apiInfo(
                ApiInfoBuilder()
                    .version(v1_0)
                    .title("API")
                    .description("Documentation backend API ${v1_0}")
                    .build()
            )
    }

    @Bean
    fun swaggerCurrentApi(): Docket? {
        return Docket(DocumentationType.OAS_30)
            .groupName("save-backend-api-${currentVersion}")
            .select()
            .apis(RequestHandlerSelectors.basePackage("org.cqfn.save.backend.controllers.*"))
            .paths(regex("\\*/${currentVersion}\\*"))
            .build()
            .apiInfo(
                ApiInfoBuilder()
                    .version(currentVersion)
                    .title("API")
                    .description("Documentation backend API ${currentVersion}")
                    .build()
            )
    }

    @Bean
    fun swaggerApi20(): Docket? {
        return Docket(DocumentationType.SWAGGER_2)
            .groupName("save-backend-api-${v2_0}")
            .select()
            .apis(RequestHandlerSelectors.basePackage("org.cqfn.save.backend.controllers.*"))
            .paths(regex("\\*/${v2_0}\\*"))
            .build()
            .apiInfo(
                ApiInfoBuilder()
                    .version(v2_0)
                    .title("API")
                    .description("Documentation backend API v2.0")
                    .build()
            )
    }

}