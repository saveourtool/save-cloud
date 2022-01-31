package org.cqfn.save.domain

/**
 * User roles
 */
enum class Role {
    /**
     * Superuser, that has access to everything
     */
    ADMIN,

    /**
     * User that has created this project
     */
    OWNER,

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
