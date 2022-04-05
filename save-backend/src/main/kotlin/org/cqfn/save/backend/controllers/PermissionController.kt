package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.configs.ApiSwaggerSupport
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.service.PermissionService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.backend.utils.toUser
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.User
import org.cqfn.save.permission.Permission
import org.cqfn.save.permission.SetRoleRequest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
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
@RequestMapping("/api/projects/roles")
@Suppress("MISSING_KDOC_ON_FUNCTION", "MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS")
class PermissionController(
    private val projectService: ProjectService,
    private val permissionService: PermissionService,
    private val organizationService: OrganizationService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
) {
    @GetMapping("/{organizationName}/{projectName}")
    @Operation(
        description = "Get role for a user on a particular project. Returns self role if no userName is set.",
        parameters = [
            Parameter(`in` = ParameterIn.HEADER, name = "X-Authorization-Source", required = true),
        ]
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched user's role")
    @ApiResponse(
        responseCode = "404", description = "Requested user or project doesn't exist or the user doesn't have enough permissions " +
                "(i.e. project is hidden from the current user)"
    )
    fun getRole(@PathVariable organizationName: String,
                @PathVariable projectName: String,
                @RequestParam(required = false) userName: String?,
                authentication: Authentication,
    ): Mono<Role?> = permissionService.findUserAndProject(
        // fixme: userName should be like that: ${user.source}:${user.name}
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

    @PostMapping("/{organizationName}/{projectName}")
    @Operation(
        description = "Set role for a user on a particular project",
        parameters = [
            Parameter(`in` = ParameterIn.HEADER, name = "X-Authorization-Source", required = true),
        ]
    )
    @ApiResponse(responseCode = "200", description = "Permission added")
    @ApiResponse(responseCode = "403", description = "User doesn't have permissions to manage this organization members")
    @ApiResponse(responseCode = "404", description = "Requested user or project doesn't exist")
    fun setRole(@PathVariable organizationName: String,
                @PathVariable projectName: String,
                @RequestBody setRoleRequest: SetRoleRequest,
                authentication: Authentication,
    ) = Mono.justOrEmpty(
        projectService.findByNameAndOrganizationName(projectName, organizationName)
            .let { Optional.ofNullable(it) }
    ).filter { project: Project ->
        // if project is hidden from the user, who attempts permission update,
        // then we should return 404
        projectPermissionEvaluator.hasPermission(authentication, project, Permission.READ)
    }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .filter {
            // fixme: could be `@PreAuthorize`, but organizationService cannot be found smh
            organizationService.canChangeRoles(organizationName, (authentication.details as AuthenticationDetails).id)
        }
        .flatMap {
            permissionService.setRole(organizationName, projectName, setRoleRequest)
        }
        .switchIfEmpty {
            logger.info("Attempt to perform role update $setRoleRequest with insufficient permissions")
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }

    companion object {
        @JvmStatic private val logger = LoggerFactory.getLogger(PermissionController::class.java)
    }
}
