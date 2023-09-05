package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.User
import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.*

/**
 * @property cosvMetadataId
 * @property user in vulnerability with [cosvMetadataId]
 */
@Entity
@Suppress("KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT")
class LnkCosvMetadataUser(
    // Fetching two or more Bags(List) at the same time on an Entity could form a Cartesian Product. Since a Bag doesn't have an order,
    // Hibernate would not be able to map the right columns to the right entities. Hence, in this case, it throws a MultipleBagFetchException.
    // Link to docs: https://www.baeldung.com/java-hibernate-multiplebagfetchexception#cause-of-multiplebagfetchexception

    var cosvMetadataId: Long,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

    ) : BaseEntity()
