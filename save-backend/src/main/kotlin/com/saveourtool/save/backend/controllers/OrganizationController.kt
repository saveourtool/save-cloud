package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.configs.ApiSwaggerSupport
import com.saveourtool.save.backend.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.backend.security.OrganizationPermissionEvaluator
import com.saveourtool.save.backend.service.GitService
import com.saveourtool.save.backend.service.LnkUserOrganizationService
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.domain.ImageInfo
import com.saveourtool.save.domain.OrganizationSaveStatus
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.entities.toOrganization
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.utils.info
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import com.saveourtool.save.utils.switchIfEmptyToResponseException
import com.saveourtool.save.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import java.time.LocalDateTime

/**
 * Controller for working with organizations.
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "organizations"),
)
@RestController
@RequestMapping(path = ["/api/$v1/organization", "/api/$v1/organizations"])
internal class OrganizationController(
    private val organizationService: OrganizationService,
    private val lnkUserOrganizationService: LnkUserOrganizationService,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
    private val gitService: GitService,
) {
    @GetMapping("/{organizationName}")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get organization by name.",
        description = "Get an organization by its name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched organization by it's name.")
    @ApiResponse(responseCode = "404", description = "Organization with such name was not found.")
    fun getOrganizationByName(
        @PathVariable organizationName: String,
    ) = Mono.fromCallable {
        organizationService.findByName(organizationName)
    }.switchIfEmptyToNotFound {
        "Organization not found by name $organizationName"
    }

    @GetMapping("/get/list")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get organizations by ownerId.",
        description = "Get list of all organizations where user with ownerId is an owner.",
        deprecated = true,
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of organizations.")
    fun getOrganizationsByOwnerId(authentication: Authentication?): Flux<Organization> {
        authentication ?: return Flux.empty()
        val ownerId = (authentication.details as AuthenticationDetails).id
        return Flux.fromIterable(organizationService.findByOwnerId(ownerId))
    }

    @GetMapping("/{organizationName}/avatar")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get avatar by organization name.",
        description = "Get organization avatar by organization name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched avatar by organization name.")
    fun avatar(
        @PathVariable organizationName: String
    ): Mono<ImageInfo> = Mono.fromCallable {
        organizationService.findByName(organizationName)?.avatar.let { ImageInfo(it) }
    }

    @PostMapping("/{organizationName}/manage-contest-permission")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Operation(
        method = "POST",
        summary = "Make an organization to be able to create contests.",
        description = "Make an organization to be able to create contests.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "isAbleToCreateContests", `in` = ParameterIn.QUERY, description = "new flag for contest creation ability", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully changed ability to create contests.")
    @ApiResponse(responseCode = "403", description = "Could not change ability to create contests due to lack of permission.")
    @ApiResponse(responseCode = "404", description = "Organization with such name was not found.")
    fun setAbilityToCreateContest(
        @PathVariable organizationName: String,
        @RequestParam isAbleToCreateContests: Boolean,
        authentication: Authentication?,
    ): Mono<StringResponse> = Mono.just(
        organizationName
    )
        .flatMap {
            organizationService.findByName(organizationName).toMono()
        }
        .switchIfEmptyToNotFound {
            "No organization with name $organizationName was found."
        }
        .filter {
            organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication!!, it.name, Role.SUPER_ADMIN)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission for managing canCreateContests flag."
        }
        .map { organization ->
            organizationService.updateOrganization(
                organization.copy(canCreateContests = isAbleToCreateContests).apply { id = organization.id }
            )
            ResponseEntity.ok("Organization updated")
        }

    @PostMapping("/save")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Create a new organization.",
        description = "Create a new organization.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved a new organization.")
    @ApiResponse(responseCode = "409", description = "Requested name is not available.")
    fun saveOrganization(
        @RequestBody newOrganization: OrganizationDto,
        authentication: Authentication?,
    ): Mono<StringResponse> = Mono.just(newOrganization)
        .map {
            organizationService.saveOrganization(it.toOrganization(LocalDateTime.now()))
        }
        .filter { (_, status) ->
            status == OrganizationSaveStatus.NEW
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            OrganizationSaveStatus.CONFLICT.message
        }
        .map { (organizationId, organizationStatus) ->
            lnkUserOrganizationService.setRoleByIds(
                (authentication?.details as AuthenticationDetails).id,
                organizationId,
                Role.OWNER,
            )
            logger.info("Save new organization id = $organizationId")
            ResponseEntity.ok(organizationStatus.message)
        }

    @PostMapping("/{organizationName}/update")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Update existing organization.",
        description = "Change settings of an existing organization by it's name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully updated an organization.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for managing this organization.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    @ApiResponse(responseCode = "409", description = "Organization with such name already exists.")
    fun updateOrganization(
        @PathVariable organizationName: String,
        @RequestBody organization: Organization,
        authentication: Authentication?,
    ): Mono<StringResponse> = Mono.just(
        organizationName
    )
        .flatMap {
            organizationService.findByName(it).toMono()
        }
        .switchIfEmptyToNotFound {
            "Could not find an organization with name $organizationName."
        }
        .filter {
            organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication!!, it.name, Role.OWNER)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission for managing organization $organizationName."
        }
        .filter {
            organizationService.findByName(organization.name) != null
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "There already is an organization with name ${organization.name}"
        }
        .map { organizationFromDb ->
            organizationService.updateOrganization(
                organization.copy(canCreateContests = organizationFromDb.canCreateContests).apply { id = organizationFromDb.id }
            )
            ResponseEntity.ok("Organization updated")
        }

    @DeleteMapping("/{organizationName}/delete")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "DELETE",
        summary = "Delete existing organization.",
        description = "Delete existing organization by its name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted an organization.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for deleting this organization.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    fun deleteOrganization(
        @PathVariable organizationName: String,
        authentication: Authentication?,
    ): Mono<StringResponse> = Mono.just(organizationName)
        .flatMap {
            organizationService.findByName(it).toMono()
        }
        .switchIfEmptyToNotFound {
            "Could not find an organization with name $organizationName."
        }
        .filter {
            organizationPermissionEvaluator.hasPermission(authentication, it, Permission.DELETE)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission for deletion of organization $organizationName."
        }
        .map {
            organizationService.deleteOrganization(it.name)
            ResponseEntity.ok("Organization deleted")
        }

    @GetMapping("/{organizationName}/list-git")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "GET",
        summary = "Get organization Gits.",
        description = "Get a list of organization's Gits.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched a list of GitDtos.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    fun listGit(
        @PathVariable organizationName: String,
        authentication: Authentication?,
    ): Flux<GitDto> = Mono.just(organizationName)
        .flatMap {
            organizationService.findByName(it).toMono()
        }
        .switchIfEmptyToNotFound {
            "Could not find an organization with name $organizationName."
        }
        .filter {
            organizationPermissionEvaluator.hasPermission(authentication, it, Permission.WRITE)
        }
        .flatMapIterable {
            gitService.getAllByOrganization(it)
        }
        .map { it.toDto() }

    @PostMapping("/{organizationName}/upsert-git")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "POST",
        summary = "Upsert organization git.",
        description = "Upsert organization git.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved an organization git.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for saving organization git.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    fun upsertGit(
        @PathVariable organizationName: String,
        @RequestBody gitDto: GitDto,
        authentication: Authentication?,
    ): Mono<StringResponse> = Mono.just(organizationName)
        .flatMap {
            organizationService.findByName(organizationName).toMono()
        }
        .switchIfEmptyToNotFound {
            "Could not find organization with name $organizationName"
        }
        .filter {
            organizationPermissionEvaluator.hasPermission(authentication, it, Permission.DELETE)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission for saving git of $organizationName."
        }
        .map {
            gitService.upsert(it, gitDto)
            ResponseEntity.ok("Git credential saved")
        }

    @DeleteMapping("/{organizationName}/delete-git")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("isAuthenticated()")
    @Operation(
        method = "DELETE",
        summary = "Upsert organization git.",
        description = "Upsert organization git.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "url", `in` = ParameterIn.QUERY, description = "url of a git", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted an organization git credentials.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for deleting organization git credentials.")
    @ApiResponse(responseCode = "404", description = "Could not find an organization with such name.")
    fun deleteGit(
        @PathVariable organizationName: String,
        @RequestParam url: String,
        authentication: Authentication?,
    ): Mono<StringResponse> = Mono.just(organizationName)
        .flatMap {
            organizationService.findByName(it).toMono()
        }
        .switchIfEmptyToNotFound {
            "Could not find an organization with name $organizationName."
        }
        .filter {
            organizationPermissionEvaluator.hasPermission(authentication, it, Permission.DELETE)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission for managing organization git credentials."
        }
        .map {
            gitService.delete(it, url)
            ResponseEntity.ok("Git credential deleted")
        }

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(OrganizationController::class.java)
    }
}
