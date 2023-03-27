package com.saveourtool.save.demo.cpg.entity

import com.fasterxml.jackson.annotation.JsonBackReference
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
class TreeSitterNode {
    @Id @GeneratedValue
    var id: Long? = null

    @Relationship(value = "SIBLING", direction = Relationship.Direction.INCOMING)
    @JsonBackReference
    var prev: TreeSitterNode? = null

    @Relationship(value = "SIBLING", direction = Relationship.Direction.OUTGOING)
    @JsonBackReference
    var next: TreeSitterNode? = null

    @Relationship(value = "PARENT", direction = Relationship.Direction.INCOMING)
    @JsonBackReference
    var parent: TreeSitterNode? = null

    @Relationship(value = "PARENT", direction = Relationship.Direction.OUTGOING)
    @JsonBackReference
    var child: MutableList<TreeSitterNode> = mutableListOf()

    @Convert(TreeSitterLocation.Companion.Converter::class)
    var location: TreeSitterLocation = TreeSitterLocation()

    var localName: String = "N/A"

    var code: String = "N/A"

    /**
     * @return [id] as not null with validating
     * @throws IllegalArgumentException when [id] is not set that means entity is not saved yet
     */
    fun requiredId(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }
}
