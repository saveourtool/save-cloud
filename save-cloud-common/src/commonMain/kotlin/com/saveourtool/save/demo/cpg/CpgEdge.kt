/**
 * File that contains CpgEdge definition
 */

package com.saveourtool.save.demo.cpg

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property key id of an edge
 * @property source the beginning of the edge
 * @property target the end of the edge
 * @property attributes displaying attributes
 */
@Serializable
@ExperimentalSerializationApi
data class CpgEdge(
    val key: String,
    val source: String,
    val target: String,
    val attributes: CpgEdgeAttributes,
)

/**
 * @property label label for edge
 * @property color edge and label color in format '#FFFFFF'
 * @property size size of edge
 */
@Serializable
@ExperimentalSerializationApi
data class CpgEdgeAttributes(
    val label: String,
    val color: String,
    @EncodeDefault(ALWAYS) val size: Int = 10,
)
