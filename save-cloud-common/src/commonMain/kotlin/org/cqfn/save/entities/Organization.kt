package org.cqfn.save.entities

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property name organization
 * @property owner organization
 * @property dateCreated date created organization
 */
@Entity
@Serializable
@Suppress("USE_DATA_CLASS")
class Organization(
    var name: String,
    var owner: Long? = null,
    var dateCreated: LocalDateTime,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null
}
