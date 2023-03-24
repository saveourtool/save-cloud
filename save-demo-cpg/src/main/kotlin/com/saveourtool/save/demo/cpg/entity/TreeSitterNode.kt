package com.saveourtool.save.demo.cpg.entity

import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.typeconversion.Convert


/**
 * Entity to store [ai.serenade.treesitter.Node]
 *
 * @property id
 * @property prev
 * @property next
 * @property parent
 * @property child
 */
@NodeEntity
data class TreeSitterNode(
    @Id @GeneratedValue
    var id: Long? = null,
    @Relationship(value = "SIBLING", direction = Relationship.Direction.INCOMING)
    var prev: TreeSitterNode? = null,
    @Relationship(value = "SIBLING", direction = Relationship.Direction.OUTGOING)
    var next: TreeSitterNode? = null,
    @Relationship(value = "PARENT", direction = Relationship.Direction.INCOMING)
    var parent: TreeSitterNode? = null,
    @Relationship(value = "PARENT", direction = Relationship.Direction.OUTGOING)
    var child: MutableList<TreeSitterNode> = mutableListOf(),
    @Convert(TreeSitterLocation.Companion.Converter::class)
    var location: TreeSitterLocation,

    var localName: String,
    var code: String,
) {
    /**
     * @return [id] as not null with validating
     * @throws IllegalArgumentException when [id] is not set that means entity is not saved yet
     */
    fun requiredId(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }
}
