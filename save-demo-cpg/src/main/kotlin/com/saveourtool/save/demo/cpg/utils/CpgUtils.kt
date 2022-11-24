package com.saveourtool.save.demo.cpg.utils

import com.saveourtool.save.demo.cpg.CpgEdge
import com.saveourtool.save.demo.cpg.CpgEdgeAttributes
import com.saveourtool.save.demo.cpg.CpgNode
import com.saveourtool.save.demo.cpg.CpgNodeAttributes
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import kotlinx.serialization.ExperimentalSerializationApi

fun PropertyEdge<*>.getId() = "${start.id}->${end.id}"

@ExperimentalSerializationApi
fun Node.toCpgNode() = CpgNode(
    id.toString(),
    CpgNodeAttributes(
        name,
    ),
)

@ExperimentalSerializationApi
fun PropertyEdge<*>.toCpgEdge() = CpgEdge(
    getId(),
    start.id.toString(),
    end.id.toString(),
    CpgEdgeAttributes(getProperty(Properties.NAME) as String),
).also { println(it) }