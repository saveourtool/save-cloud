package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.StringResponse
import org.cqfn.save.backend.security.OrganizationPermissionEvaluator
import org.cqfn.save.backend.service.LnkUserOrganizationService
import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.domain.OrganizationSaveStatus
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Organization
import org.cqfn.save.v1
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
import java.time.LocalDateTime

/**
 * Controller for working with organizations.
 */
@RestController
@RequestMapping(path = ["/api/$v1/organization"])
internal class OrganizationController(
    private val organizationService: OrganizationService,
    private val lnkUserOrganizationService: LnkUserOrganizationService,
    private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
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
    fun updateOrganization(@RequestBody organization: Organization, authentication: Authentication): Mono<StringResponse> = Mono
        .just(organization)
        .filter { organizationPermissionEvaluator.hasGlobalRoleOrOrganizationRole(authentication, it.name, Role.OWNER) }
        .map {
            organizationService.updateOrganization(organization)
            ResponseEntity.ok("Organization updated")
        }
        .defaultIfEmpty(ResponseEntity.status(HttpStatus.FORBIDDEN).build())

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

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(OrganizationController::class.java)
    }
}
