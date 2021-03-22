package org.cqfn.save.entities

import kotlinx.serialization.Serializable

/**
 * @property owner
 * @property name
 * @property type type of the project, e.g. github or manually registered
 * @property url
 * @property description description of the project, may be absent
 */
@Entity
@Serializable
class Project(
    var owner: String,
    var name: String,
    var type: String,
    var url: String,
    var description: String?,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    /**
     * Override equals to compare projects without id
     *
     * @param other other projcet
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        other?.let { anotherProject ->
            if (anotherProject::class != this::class) return false
            (anotherProject as Project).also {
                return (it.description == this.description &&
                        it.name == this.name &&
                        it.owner == this.owner &&
                        it.type == this.type &&
                        it.url == this.url)
            }
        } ?: return false
        return super.equals(other)
    }

    /**
     * Override hashCode
     */
    override fun hashCode(): Int {
        return super.hashCode()
    }
}
