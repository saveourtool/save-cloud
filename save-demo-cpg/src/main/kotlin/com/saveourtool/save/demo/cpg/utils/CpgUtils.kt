@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.demo.cpg.utils

import com.saveourtool.save.demo.cpg.CpgEdge
import com.saveourtool.save.demo.cpg.CpgEdgeAttributes
import com.saveourtool.save.demo.cpg.CpgNode
import com.saveourtool.save.demo.cpg.CpgNodeAttributes

import de.fraunhofer.aisec.cpg.graph.Node
import org.neo4j.ogm.response.model.RelationshipModel

import kotlinx.serialization.ExperimentalSerializationApi

/**
 * @return [CpgNode] from [Node]
 */
@ExperimentalSerializationApi
fun Node.toCpgNode() = CpgNode(
    id.toString(),
    CpgNodeAttributes(
        name,
    ),
)

/**
 * @return [CpgEdge] from [RelationshipModel]
 */
@ExperimentalSerializationApi
fun RelationshipModel.toCpgEdge() = CpgEdge(
    id.toString(),
    startNode.toString(),
    endNode.toString(),
    CpgEdgeAttributes(
        this.type,
    ),
)
