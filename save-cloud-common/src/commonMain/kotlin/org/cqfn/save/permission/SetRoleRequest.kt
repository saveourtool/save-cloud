package org.cqfn.save.permission

import org.cqfn.save.domain.Role

import kotlinx.serialization.Serializable

/**
 * @property userName user whose role will be updated
 * @property role a new role
 */
@Serializable
data class SetRoleRequest(
    val userName: String,
    val role: Role,
)
