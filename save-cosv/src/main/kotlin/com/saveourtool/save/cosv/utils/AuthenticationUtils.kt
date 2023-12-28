@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.cosv.utils

import com.saveourtool.save.domain.Role
import org.springframework.security.core.Authentication

/**
 * Check role out of [Authentication]
 *
 * @param role
 * @return true if user with [Authentication] has [role], false otherwise
 */
fun Authentication.hasRole(role: Role): Boolean = authorities.any { it.authority == role.asSpringSecurityRole() }
