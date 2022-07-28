package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.StringResponse
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
import com.saveourtool.save.v1
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.time.LocalDateTime
import java.util.*

/**
 * Controller for working with organizations.
 */
@RestController
@RequestMapping(path = ["/api/$v1/organization", "/api/$v1/organizations"])
internal class OrganizationController(
    private val organizationService: OrganizationService,
    private val lnkUserOrganizationService: LnkUserOrganizationService,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
    private val gitService: GitService,
) {
    /**
     * @param organizationName
     * @return Organization
     */
    @GetMapping("/{organizationName}")
    @PreAuthorize("permitAll()")
    fun getOrganizationByName(@PathVariable organizationName: String) = Mono.fromCallable {
        organizationService.findByName(organizationName)
    }.switchIfEmpty {
        Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
    }

    /**
     * @param authentication an [Authentication] representing an authenticated request
     * @return list of organization by owner id
     */
    @GetMapping("/get/list")
    @PreAuthorize("permitAll()")
    fun getOrganizationsByOwnerId(authentication: Authentication?): Flux<Organization> {
        authentication ?: return Flux.empty()
        val ownerId = (authentication.details as AuthenticationDetails).id
        return Flux.fromIterable(organizationService.findByOwnerId(ownerId))
    }

    /**
     * @param organizationName organization name
     * @return [ImageInfo] about organization's avatar
     */
    @GetMapping("/{organizationName}/avatar")
    @PreAuthorize("permitAll()")
    fun avatar(@PathVariable organizationName: String): Mono<ImageInfo> = Mono.fromCallable {
        organizationService.findByName(organizationName)?.avatar.let { ImageInfo(it) }
    }

    /**
     * @param organizationName name of an organization
     * @param isAbleToCreateContests new value of flag Organization.canCreateContests
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    @PostMapping("/{organizationName}/manage-contest-permission")
    @Suppress("UnsafeCallOnNullableType")
    fun setAbilityToCreateContest(
        @PathVariable organizationName: String,
        @RequestParam isAbleToCreateContests: Boolean,
        authentication: Authentication
    ): Mono<StringResponse> = Mono.just(
        organizationName
    )
        .flatMap {
            organizationService.findByName(organizationName).toMono()
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .filter {
            organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication, it.name, Role.SUPER_ADMIN)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .map { organization ->
            organizationService.setAbilityToCreateContest(
                organization.copy(canCreateContests = isAbleToCreateContests).apply { id = organization.id }
            )
            ResponseEntity.ok("Organization updated")
        }

    /**
     * @param newOrganization newOrganization
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    fun saveOrganization(@RequestBody newOrganization: Organization, authentication: Authentication): Mono<StringResponse> {
        val ownerId = (authentication.details as AuthenticationDetails).id
        val (organizationId, organizationStatus) = organizationService.getOrSaveOrganization(
            newOrganization.apply {
                this.ownerId = ownerId
                this.dateCreated = LocalDateTime.now()
            }
        )
        if (organizationStatus == OrganizationSaveStatus.NEW) {
            lnkUserOrganizationService.setRoleByIds(ownerId, organizationId, Role.OWNER)
        }

        val response = if (organizationStatus == OrganizationSaveStatus.EXIST) {
            logger.info("Attempt to save an organization with id = $organizationId, but it already exists.")
            ResponseEntity.badRequest().body(organizationStatus.message)
        } else {
            logger.info("Save new organization id = $organizationId with ownerId $ownerId")
            ResponseEntity.ok(organizationStatus.message)
        }
        return Mono.just(response)
    }

    /**
     * @param organization updateOrganization
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    @PostMapping("/{organizationName}/update")
    @Suppress("UnsafeCallOnNullableType")
    fun updateOrganization(
        @RequestBody organization: Organization,
        authentication: Authentication,
    ): Mono<StringResponse> = Mono.just(
        organization.name
    )
        .flatMap {
            organizationService.findByName(organization.name).toMono()
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }
        .filter {
            organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication, organization.name, Role.OWNER)
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN))
        }
        .map { organizationFromDb ->
            organizationService.updateOrganization(
                organization.copy(canCreateContests = organizationFromDb.canCreateContests).apply { id = organizationFromDb.id }
            )
            ResponseEntity.ok("Organization updated")
        }

    /**
     * @param organizationName
     * @param authentication
     */
    @DeleteMapping("/{organizationName}/delete")
    fun deleteOrganization(@PathVariable organizationName: String, authentication: Authentication) = Mono
        .just(organizationName)
        .filter { organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication, it, Role.OWNER) }
        .map {
            organizationService.deleteOrganization(it)
            ResponseEntity.ok("Organization deleted")
        }
        .defaultIfEmpty(ResponseEntity.status(HttpStatus.FORBIDDEN).build())

    /**
     * @param organizationName
     * @param authentication
     * @return list of [GitDto] associated with [organizationName]
     */
    @GetMapping("/{organizationName}/list-git")
    fun listGit(
        @PathVariable organizationName: String,
        authentication: Authentication
    ): Flux<GitDto> = Mono
        .just(organizationName)
        .filter { organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication, it, Role.VIEWER) }
        .map { organizationService.getByName(it) }
        .flatMapIterable { gitService.getAllByOrganization(it) }
        .map { it.toDto() }

    /**
     * @param organizationName
     * @param gitDto
     * @param authentication
     * @return result of operation
     */
    @PostMapping("/{organizationName}/upsert-git")
    fun upsertGit(
        @PathVariable organizationName: String,
        @RequestBody gitDto: GitDto,
        authentication: Authentication
    ): Mono<StringResponse> = Mono
        .just(organizationName)
        .filter { organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication, it, Role.OWNER) }
        .map { organizationService.getByName(it) }
        .map {
            gitService.upsert(it, gitDto)
            ResponseEntity.ok("Git credential saved")
        }
        .defaultIfEmpty(ResponseEntity.status(HttpStatus.FORBIDDEN).build())

    /**
     * @param organizationName
     * @param url
     * @param authentication
     * @return result of operation
     */
    @DeleteMapping("/{organizationName}/delete-git")
    fun deleteGit(@PathVariable organizationName: String,
                  @RequestParam url: String,
                  authentication: Authentication
    ): Mono<StringResponse> = Mono.just(organizationName)
        .filter { organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication, it, Role.OWNER) }
        .map { organizationService.getByName(it) }
        .map {
            gitService.delete(it, url)
            ResponseEntity.ok("Git credential deleted")
        }
        .defaultIfEmpty(ResponseEntity.status(HttpStatus.FORBIDDEN).build())

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(OrganizationController::class.java)
    }
}
