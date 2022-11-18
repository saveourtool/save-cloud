/**
 * File that contains CpgNode definition
 */
package com.saveourtool.save.demo.cpg

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property key id of a node
 * @property attributes displaying attributes
 */
@Serializable
@ExperimentalSerializationApi
data class CpgNode(
    val key: String,
    val attributes: CpgNodeAttributes,
)

/**
 * @property label node label
 * @property color node color
 * @property size size of a node
 * @property x x coordinate - will be applied by layout
 * @property y y coordinate - will be applied by layout
 */
@Serializable
@ExperimentalSerializationApi
data class CpgNodeAttributes(
    val label: String,
    val color: String,
    @EncodeDefault(ALWAYS) val size: Long = 20,
    @EncodeDefault(ALWAYS) val x: Long = 0,
    @EncodeDefault(ALWAYS) val y: Long = 0,
)
