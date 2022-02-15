package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.entities.Organization
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
            organizationService.findByName(name)
}
