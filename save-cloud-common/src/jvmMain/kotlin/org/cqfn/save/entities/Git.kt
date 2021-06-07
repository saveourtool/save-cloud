package org.cqfn.save.entities

import javax.persistence.Entity
import javax.persistence.JoinColumn
import kotlinx.serialization.Serializable
import javax.persistence.OneToOne

/**
 * Data class with repository information
 * fixme should operate not with password, but with some sort of token (github integration)
 *
 * @property url url of repo
 * @property username username to credential
 * @property password password to credential
 * @property branch branch to clone
 */
@Entity
@Serializable
data class Git(
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val branch: String? = null,

    @OneToOne
    @JoinColumn(name = "project_id")
    val project: Project,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    fun toDto() = GitDto(
        this.url,
        this.username,
        this.password,
        this.branch,
        this.project
    )
}
