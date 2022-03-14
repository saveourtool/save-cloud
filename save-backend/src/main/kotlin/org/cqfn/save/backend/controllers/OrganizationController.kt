package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.domain.OrganizationSaveStatus
import org.cqfn.save.entities.Organization
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
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
     * @param name
     * @return Organization
     */
    @GetMapping("/get/organization-name")
    fun getOrganizationByName(@RequestParam name: String): Organization =
            organizationService.findByName(name) ?: throw NoSuchElementException("Organization with name [$name] was not found.")

    /**
     * @param owner owner name
     * @return a image
     */
    @GetMapping("/avatar")
    fun avatar(@RequestParam owner: String): ImageInfo? = organizationService.findByName(owner)?.avatar.let { ImageInfo(it) }

    /**
     * @param newOrganization newOrganization
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    @PostMapping("/save")
    fun saveOrganization(@RequestBody newOrganization: Organization, authentication: Authentication): ResponseEntity<String> {
        val ownerId = (authentication.details as AuthenticationDetails).id
        val (organizationId, organizationStatus) = organizationService.getOrSaveOrganization(
            newOrganization.apply {
                this.ownerId = ownerId
                this.dateCreated = LocalDateTime.now()
            }
        )

        if (organizationStatus == OrganizationSaveStatus.EXIST) {
            logger.warn("Organization with id = $organizationId already exists")
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
