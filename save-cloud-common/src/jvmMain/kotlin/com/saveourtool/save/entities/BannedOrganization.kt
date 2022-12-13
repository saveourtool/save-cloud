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
data class BannedOrganization(
    var name: String,
    var privateComment: String,
    var publicComment: String,
) {

}

fun Organization.makeBanned(privateComment: String, publicComment: String?) =
    BannedOrganization(this.name, privateComment, publicComment ?: privateComment)