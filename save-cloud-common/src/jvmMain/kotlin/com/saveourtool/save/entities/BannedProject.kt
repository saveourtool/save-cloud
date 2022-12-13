package com.saveourtool.save.entities

import kotlinx.serialization.Serializable
import javax.persistence.Entity

/**
 * @property name
 * @property privateComment
 * @property publicComment
 */
@Entity
@Serializable
data class BannedProject(
    var name: String,
    var privateComment: String,
    var publicComment: String,
) {
    var id: Long = 1
}

fun Project.makeBanned(privateComment: String, publicComment: String?) =
    BannedOrganization(this.name, privateComment, publicComment ?: privateComment)