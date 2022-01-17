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
     * Has write access to project, can start executions.
     */
    PROJECT_ADMIN,

    /**
     * User that created a project. Can modify and delete the project and invite admins.
     */
    PROJECT_OWNER,

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
