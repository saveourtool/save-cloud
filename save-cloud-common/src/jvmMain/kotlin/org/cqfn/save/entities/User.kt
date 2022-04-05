package org.cqfn.save.entities

import org.cqfn.save.domain.Role
import javax.persistence.Entity

/**
 * @property name
 * @property password *in plain text*
 * @property role role of this user
 * @property source where the user identity is coming from, e.g. "github"
 */
@Entity
class User(
    var name: String?,
    var password: String?,
    var role: String?,
    var source: String,
) : BaseEntity() {
    /**
     * @param projects
     * @return [UserDto] object
     */
    fun toDto(projects: Map<String, Role?> = emptyMap()) = UserDto(
        name = name,
        source = source,
        projects = projects,
    )
}
