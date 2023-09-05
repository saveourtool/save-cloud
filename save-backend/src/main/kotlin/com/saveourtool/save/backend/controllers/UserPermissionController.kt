package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.security.UserPermissionEvaluator
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.info.UserPermissions
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.v1
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller for user permissions.
 */
@ApiSwaggerSupport
@RestController
@RequestMapping(path = ["/api/$v1"])
class UserPermissionController(
    private val userPermissionEvaluator: UserPermissionEvaluator,
) {
    /**
     * @param authentication
     */
    @GetMapping("/users/permissions")
    fun getUserPermissions(
        authentication: Authentication,
    ): Mono<UserPermissions> = blockingToMono {
        userPermissionEvaluator.getUserPermissions(authentication)
    }

    /**
     * @param authentication
     * @param organizationName
     */
    @GetMapping("/users/permissions-by-organization")
    fun getUserPermissions(
        authentication: Authentication,
        @RequestParam organizationName: String,
    ): Mono<UserPermissions> = blockingToMono {
        userPermissionEvaluator.getUserPermissions(authentication, organizationName)
    }
}
