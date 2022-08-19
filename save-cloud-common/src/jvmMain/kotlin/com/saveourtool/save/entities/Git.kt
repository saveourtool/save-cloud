package com.saveourtool.save.entities

import javax.persistence.Entity
import javax.persistence.JoinColumn

/**
 * Data class with repository information
 * fixme should operate not with password, but with some sort of token (github integration)
 *
 * @property url url of repo
 * @property username username to credential
 * @property password password to credential
 * @property organization
 */
@Entity
class Git(
    var url: String,
    var username: String? = null,
    var password: String? = null,

    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization,
) : BaseEntity() {
    /**
     * @return git dto
     */
    fun toDto() = GitDto(
        url = url,
        username = username,
        password = password,
    )
    companion object {
        val empty = Git("", null, null, Organization.stub(-1))
    }
}
