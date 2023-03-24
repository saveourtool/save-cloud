@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.demo.cpg.utils

import com.saveourtool.save.demo.cpg.*

import de.fraunhofer.aisec.cpg.graph.Node
import org.neo4j.ogm.response.model.RelationshipModel

import kotlinx.serialization.ExperimentalSerializationApi
import org.neo4j.ogm.response.model.NodeModel

/**
 * @return [CpgNode] from [Node]
 */
@ExperimentalSerializationApi
fun Node.toCpgNode() = CpgNode(
    id.toString(),
    CpgNodeAttributes(
        name.localName,
        additionalInfo = CpgNodeAdditionalInfo(
            code = code,
            comment = comment,
            location = location?.toString(),
            file = file,
            isInferred = isInferred,
            isImplicit = isImplicit,
            argumentIndex = argumentIndex,
        )
    ),
)

/**
 * @return [CpgNode] from [NodeModel]
 */
@ExperimentalSerializationApi
fun NodeModel.toCpgNode() = CpgNode(
    id.toString(),
    CpgNodeAttributes(
        property("localName").toString(),
        additionalInfo = CpgNodeAdditionalInfo(
            code = property("code")?.toString(),
            comment = property("comment")?.toString(),
            location = property("location")?.toString(),
            file = property("file")?.toString(),
            isInferred = property("isInferred")?.toString().toBoolean(),
            isImplicit = property("isImplicit")?.toString().toBoolean(),
            argumentIndex = property("argumentIndex")?.toString()?.toInt() ?: -1,
        )
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
