package org.cqfn.save.domain

import kotlinx.serialization.Serializable

/**
 * User roles
 * @property formattedName string representation of the [Role] that should be printed
 * @property priority
 */
@Suppress("MAGIC_NUMBER", "MagicNumber")
@Serializable
enum class Role(val formattedName: String, val priority: Int) {
    /**
     * admin in organization
     */
    ADMIN("Admin", 2),

    /**
     * Has no role (synonym to null)
     */
    NONE("None", 0),

    /**
     * User that has created this project
     */
    OWNER("Owner", 3),

    /**
     * Superuser, that has access to everything
     */
    SUPER_ADMIN("Superadmin", 4),

    /**
     * Has readonly access to public projects.
     */
    VIEWER("Viewer", 1),
    ;

    /**
     * @return this role with default prefix for spring-security
     */
    fun asSpringSecurityRole() = "ROLE_$name"

    /**
     * Minimal possible priority
     */
}

/**
 * The method compares the priority between two roles
 *
 * @param that role to compare
 * @return comparison result
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
fun Role.moreOrEqualThan(that: Role) = this.priority >= that.priority
