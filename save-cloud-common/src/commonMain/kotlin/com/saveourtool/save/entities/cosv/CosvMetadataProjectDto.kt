package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.vulnerability.VulnerabilityProjectType
import kotlinx.serialization.Serializable

/**
 * @property name name of project
 * @property url url of project
 * @property type type of link
 * @property vulnerabilityIdentifier vulnerability identifier
 * @property versions
 */
@Serializable
data class CosvMetadataProjectDto(
    val name: String,
    val url: String,
    val versions: String,
    val type: VulnerabilityProjectType,
    val vulnerabilityIdentifier: String,
) {
    companion object {
        val empty = CosvMetadataProjectDto(
            "",
            "",
            "",
            VulnerabilityProjectType.LIBRARY,
            "",
        )
    }
}
