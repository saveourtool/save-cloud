package org.cqfn.save.domain

/**
 * User roles
 * @property string
 * @property priority
 */
@Suppress("MAGIC_NUMBER", "MagicNumber")
enum class Role(val string: String, val priority: Int) {
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
