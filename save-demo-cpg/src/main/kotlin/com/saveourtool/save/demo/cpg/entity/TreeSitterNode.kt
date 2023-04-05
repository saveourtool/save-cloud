package com.saveourtool.save.demo.cpg.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.typeconversion.Convert

/**
 * Entity to store [io.github.oxisto.kotlintree.jvm.Node]
 */
@NodeEntity
class TreeSitterNode {
    /**
     * ID which NEO4j generates
     */
    @Id @GeneratedValue
    var id: Long? = null

    /**
     * Previous node on one level horizontally
     */
    @Relationship(value = "SIBLING", direction = Relationship.Direction.INCOMING)
    @JsonBackReference
    var prev: TreeSitterNode? = null

    /**
     * Next node on one level horizontally
     */
    @Relationship(value = "SIBLING", direction = Relationship.Direction.OUTGOING)
    @JsonBackReference
    var next: TreeSitterNode? = null

    /**
     * Node on one level up
     */
    @Relationship(value = "PARENT", direction = Relationship.Direction.INCOMING)
    @JsonBackReference
    var parent: TreeSitterNode? = null

    /**
     * All node on one level down
     */
    @Relationship(value = "PARENT", direction = Relationship.Direction.OUTGOING)
    @JsonBackReference
    @Suppress("DoubleMutabilityForCollection")
    var child: MutableList<TreeSitterNode> = mutableListOf()

    /**
     * Location of this node
     */
    @Convert(TreeSitterLocation.Companion.Converter::class)
    var location: TreeSitterLocation = TreeSitterLocation()

    /**
     * Local name -- probably type of node
     */
    var localName: String = "N/A"

    /**
     * A code of this node in parsed AST tree
     */
    var code: String = "N/A"

    /**
     * @return [id] as not null with validating
     * @throws IllegalArgumentException when [id] is not set that means entity is not saved yet
     */
    fun requiredId(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }
}
