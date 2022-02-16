package org.cqfn.save.domain

/**
 * User roles
 */
enum class Role {
    /**
     * admin in organization
     */
    ADMIN,

    /**
     * User that has created this project
     */
    OWNER,

    /**
     * Superuser, that has access to everything
     */
    SUPER_ADMIN,

    /**
     * Has readonly access to public projects.
     */
    VIEWER,
    ;

    /**
     * @return this role with default prefix for spring-security
     */
    fun asSpringSecurityRole() = "ROLE_$name"
}
