package com.saveourtool.save.cosv.security

import com.saveourtool.save.cosv.repository.RawCosvFileRepository

import com.saveourtool.save.permission.Permission
import com.saveourtool.save.utils.getByIdOrNotFound
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * Class that is capable of assessing user permissions.
 */
@Component
class RawCosvFilePermissionEvaluator(
    private val rawCosvFileRepository: RawCosvFileRepository,
    // private val organizationPermissionEvaluator: OrganizationPermissionEvaluator,
) {
    /**
     * Check permission for user to read, write and delete raw COSV files by its [rawCosvFileId]
     *
     * @param authentication
     * @param rawCosvFileId
     * @param permission
     * @return true if user with [authentication] has [permission] for [rawCosvFileId]
     */
    fun hasPermission(
        authentication: Authentication?,
        rawCosvFileId: Long,
        permission: Permission,
    ): Boolean {
        authentication ?: return false

        val organization = rawCosvFileRepository.getByIdOrNotFound(rawCosvFileId).organization

        return false
        // return when {
        // authentication.hasRole(Role.SUPER_ADMIN) -> true
        // permission == Permission.READ -> organizationPermissionEvaluator.hasPermission()
        // else -> hasFullPermission(vulnerabilityIdentifier, authentication)
        // }
    }
}
