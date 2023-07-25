package com.saveourtool.save.authservice.security

import org.springframework.security.core.AuthenticatedPrincipal

/**
 * @param id
 * @param name
 */
data class SaveUserPrincipal(
    val id: Long,
    val name: String,
) : AuthenticatedPrincipal {
    override fun getName(): String = this.name
}
