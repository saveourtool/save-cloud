package com.saveourtool.save.demo.cpg.entity

import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.NodeEntity

/**
 * Entity to store IDs for query
 *
 * @property id
 * @property nodeIds list of IDs from query
 */
@NodeEntity
open class DemoQuery(
    @Id @GeneratedValue
    var id: Long? = null,
    var nodeIds: Set<Long> = emptySet(),
) {
    /**
     * @return [id] as not null with validating
     * @throws IllegalArgumentException when [id] is not set that means entity is not saved yet
     */
    open fun requiredId(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }
}
