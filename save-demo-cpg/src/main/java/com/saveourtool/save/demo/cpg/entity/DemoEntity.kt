package com.saveourtool.save.demo.cpg.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship

/**
 * @property value
 * @property next
 * @property prev
 */
@Node
open class DemoEntity(
    var value: String,
) {
    /**
     * generate a unique id
     */
    @Id
    @GeneratedValue
    open var id: Long? = null

    @Relationship("NEXT")
    @JsonSerialize(contentUsing = DemoEntitySerializer::class)
    var next: MutableSet<DemoEntity> = mutableSetOf()

    @Relationship("PREV")
    @JsonSerialize(contentUsing = DemoEntitySerializer::class)
    var prev: MutableSet<DemoEntity> = mutableSetOf()
}
