package com.saveourtool.save.demo.cpg.cytoscape

import com.saveourtool.save.demo.cpg.CpgNodeAdditionalInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property data
 * @property position
 * @property additionalInfo
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CytoscapeNode(
    val data: Data,
    val position: Position? = null,
    val additionalInfo: CpgNodeAdditionalInfo? = null,
) {
    /**
     * @property x
     * @property y
     */
    @Serializable
    data class Position(val x: Int, val y: Int)

    /**
     * @property id
     * @property parent
     * @property label
     */
    @Serializable
    data class Data(val id: String, val parent: String? = null, val label: String? = null)
}
