package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.domain.OrganizationSaveStatus
import org.cqfn.save.entities.Organization
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * Controller for working with organizations.
 */
@RestController
@RequestMapping("/api/organization")
internal class OrganizationController(private val organizationService: OrganizationService) {
    /**
     * @param organizationName
     * @return Organization
     */
    @GetMapping("/{organizationName}")
    @PreAuthorize("permitAll()")
    fun getOrganizationByName(@PathVariable organizationName: String): Organization =
            organizationService.findByName(organizationName) ?: throw NoSuchElementException("Organization with name [$organizationName] was not found.")

    /**
     * @param authentication an [Authentication] representing an authenticated request
     * @return list of organization by owner id
     */
    @GetMapping("/get/list")
    @PreAuthorize("permitAll()")
    fun getOrganizationsByOwnerId(authentication: Authentication?): List<Organization> {
        authentication ?: return emptyList()
        val ownerId = (authentication.details as AuthenticationDetails).id
        return organizationService.findByOwnerId(ownerId)
    }

    /**
     * @param organizationName organization name
     * @return [ImageInfo] about organization's avatar
     */
    @GetMapping("/{organizationName}/avatar")
    @PreAuthorize("permitAll()")
    fun avatar(@PathVariable organizationName: String): ImageInfo = organizationService.findByName(organizationName)?.avatar.let { ImageInfo(it) }

    /**
     * @param newOrganization newOrganization
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    fun saveOrganization(@RequestBody newOrganization: Organization, authentication: Authentication): ResponseEntity<String> {
        val ownerId = (authentication.details as AuthenticationDetails).id
        val (organizationId, organizationStatus) = organizationService.getOrSaveOrganization(
            newOrganization.apply {
                this.ownerId = ownerId
                this.dateCreated = LocalDateTime.now()
            }
        )

        if (organizationStatus == OrganizationSaveStatus.EXIST) {
            logger.info("Attempt to save an organization with id = $organizationId, but it already exists.")
            return ResponseEntity.badRequest().body(organizationStatus.message)
        }
        logger.info("Save new organization id = $organizationId")
        return ResponseEntity.ok(organizationStatus.message)
    }

    companion object {
        @JvmStatic
        private val logger = LoggerFactory.getLogger(OrganizationController::class.java)
    }
}
