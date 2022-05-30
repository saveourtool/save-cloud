package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ApiSwaggerSupport
import com.saveourtool.save.backend.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.backend.security.OrganizationPermissionEvaluator
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.PermissionService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.utils.toUser
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.User
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.permission.SetRoleRequest
import com.saveourtool.save.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.util.Optional

@ApiSwaggerSupport
@Tags(Tag(name = "api"), Tag(name = "permissions"))
@RestController
@RequestMapping(path = ["/api/$v1"])
@Suppress("MISSING_KDOC_ON_FUNCTION", "MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS")
class PermissionController(
    private val projectService: ProjectService,
    private val permissionService: PermissionService,
    private val organizationService: OrganizationService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
) {
    @GetMapping("/projects/{organizationName}/{projectName}/users/roles")
    @Operation(
        description = "Get role for a user on a particular project. Returns self role if no userName is set.",
    )
    @RequiresAuthorizationSourceHeader
    @ApiResponse(responseCode = "200", description = "Successfully fetched user's role")
    @ApiResponse(
        responseCode = "404", description = "Requested user or project doesn't exist or the user doesn't have enough permissions " +
                "(i.e. project is hidden from the current user)"
    )
    @Suppress("UnsafeCallOnNullableType")
    fun getRole(@PathVariable organizationName: String,
                @PathVariable projectName: String,
                // fixme: userName should be like that: ${user.source}:${user.name}
                @RequestParam(required = false) userName: String?,
                authentication: Authentication,
    ): Mono<Role?> = permissionService.findUserAndProject(
        userName ?: authentication.toUser().name!!,
        organizationName,
        projectName,
    )
        .filter { (_, project) ->
            // To be able to see roles, the user should be at least `VIEWER` for public projects
            // or should have read access (i.e. be a member of) for a private project.
            projectPermissionEvaluator.hasPermission(authentication, project, Permission.READ)
        }
        .map { (user: User, project: Project) ->
            permissionService.getRole(user, project)
                .also {
                    logger.trace("User ${user.source}:${user.name} has role $it")
                }
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }

    @PostMapping("/projects/{organizationName}/{projectName}/users/roles")
    @Operation(
        description = "Set role for a user on a particular project",
    )
    @RequiresAuthorizationSourceHeader
    @ApiResponse(responseCode = "200", description = "Permission added")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or project doesn't exist")
    fun setRole(@PathVariable organizationName: String,
                @PathVariable projectName: String,
                @RequestBody setRoleRequest: SetRoleRequest,
                authentication: Authentication,
    ) = Mono.justOrEmpty(
        projectService.findByNameAndOrganizationName(projectName, organizationName)
            .let { Optional.ofNullable(it) }
    )
        .filter { project: Project ->
            // if project is hidden from the user, who attempts permission update,
            // then we should return 404
            projectPermissionEvaluator.hasPermission(authentication, project, Permission.READ)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .zipWith(Mono.justOrEmpty(projectService.findUserByName(setRoleRequest.userName)))
        .switchIfEmpty {
            Mono.error((ResponseStatusException(HttpStatus.NOT_FOUND)))
        }
        .filter { (project, user) ->
            // fixme: could be `@PreAuthorize`, but organizationService cannot be found smh
            val organization = organizationService.findByName(organizationName)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
            val hasOrganizationPermissions = organizationPermissionEvaluator.canChangeRoles(organization, authentication, user, setRoleRequest.role)
            val hasProjectPermissions = projectPermissionEvaluator.canChangeRoles(project, authentication, user, setRoleRequest.role)
            hasOrganizationPermissions || hasProjectPermissions
        }
        .flatMap {
            permissionService.setRole(organizationName, projectName, setRoleRequest)
        }
        .switchIfEmpty {
            logger.info("Attempt to perform role update $setRoleRequest with insufficient permissions")
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }

    @DeleteMapping("/projects/{organizationName}/{projectName}/users/roles/{userName}")
    @Operation(
        description = "Removes user's role on a particular project",
    )
    @RequiresAuthorizationSourceHeader
    @ApiResponse(responseCode = "200", description = "Permission removed")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this members")
    @ApiResponse(responseCode = "404", description = "Requested user or project doesn't exist")
    fun removeRole(@PathVariable organizationName: String,
                   @PathVariable projectName: String,
                   @PathVariable userName: String,
                   authentication: Authentication,
    ) = Mono.justOrEmpty(
        projectService.findByNameAndOrganizationName(projectName, organizationName)
            .let { Optional.ofNullable(it) }
    ).filter { project: Project ->
        projectPermissionEvaluator.hasPermission(authentication, project, Permission.READ)
    }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .zipWith(Mono.justOrEmpty(projectService.findUserByName(userName)))
        .switchIfEmpty {
            Mono.error((ResponseStatusException(HttpStatus.NOT_FOUND)))
        }
        .filter { (project, user) ->
            val organization = organizationService.findByName(organizationName)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
            val hasOrganizationPermissions = organizationPermissionEvaluator.canChangeRoles(organization, authentication, user)
            val hasProjectPermissions = projectPermissionEvaluator.canChangeRoles(project, authentication, user)
            hasOrganizationPermissions || hasProjectPermissions
        }
        .switchIfEmpty {
            logger.info("Attempt to remove $userName from $organizationName/$projectName with insufficient permissions")
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .flatMap {
            permissionService.removeRole(organizationName, projectName, userName)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }

    companion object {
        @JvmStatic private val logger = LoggerFactory.getLogger(PermissionController::class.java)
    }
}
