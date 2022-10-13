package com.saveourtool.save.permission

import com.saveourtool.save.domain.Role

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
