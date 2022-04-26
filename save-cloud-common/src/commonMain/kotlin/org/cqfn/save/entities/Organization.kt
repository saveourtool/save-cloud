package org.cqfn.save.entities

import org.cqfn.save.utils.LocalDateTime

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * @property name organization
 * @property ownerId organization
 * @property dateCreated date created organization
 * @property avatar
 * @property description
 */
@Entity
@Serializable
@Suppress("USE_DATA_CLASS")
data class Organization(
    var name: String,
    var ownerId: Long? = null,
    @Contextual
    var dateCreated: LocalDateTime?,
    var avatar: String? = null,
    var description: String? = null,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null
}
