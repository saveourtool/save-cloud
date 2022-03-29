package org.cqfn.save.entities

import kotlinx.serialization.Serializable
import org.cqfn.save.domain.Role

/**
 * @property name
 * @property source
 * @property projects
 * @property source where the user identity is coming from, e.g. "github"
 */
@Serializable
data class UserDto(
    val name: String?,
    var source: String,
    var projects: Map<String, Role?>,
)
