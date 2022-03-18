package org.cqfn.save.backend.configs

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme

/**
 * Meta-annotation that sets common swagger annotations for a `/api` controller.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SecurityScheme(name = "basic", type = SecuritySchemeType.HTTP, scheme = "basic")
@SecurityRequirement(name = "basic")
annotation class ApiSwaggerSupport
