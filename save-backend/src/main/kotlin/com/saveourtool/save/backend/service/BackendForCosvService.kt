package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.service.vulnerability.VulnerabilityService
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.vulnerabilities.Vulnerability
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import org.springframework.stereotype.Service

/**
 * Service for [IBackendService] to get required info for COSV from backend
 */
@Service
class BackendForCosvService(
    private val vulnerabilityService: VulnerabilityService,
    private val organizationService: OrganizationService,
    private val userDetailsService: UserDetailsService,
) : IBackendService {
    override fun findVulnerabilityByName(name: String): Vulnerability? = vulnerabilityService.findByName(name)

    override fun saveVulnerability(vulnerabilityDto: VulnerabilityDto, user: User): Vulnerability = vulnerabilityService.save(vulnerabilityDto, user)

    override fun getOrganizationByName(name: String): Organization = organizationService.getByName(name)

    override fun getUserByName(name: String): User = userDetailsService.getByName(name)
}
