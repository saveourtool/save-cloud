package org.cqfn.save.permission

import kotlinx.serialization.Serializable
import org.cqfn.save.domain.Role

@Serializable
data class SetRoleRequest(
    val projectName: String,
    val organizationName: String,
    val userName: String,
    val role: Role,
)
