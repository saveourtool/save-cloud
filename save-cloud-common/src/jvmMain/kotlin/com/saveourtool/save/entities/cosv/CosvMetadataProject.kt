package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.vulnerability.VulnerabilityProjectType
import com.saveourtool.save.spring.entity.BaseEntityWithDto

import com.fasterxml.jackson.annotation.JsonBackReference
import com.saveourtool.save.entities.vulnerability.VulnerabilityProjectDto

import javax.persistence.*

/**
 * Class with projects of common vulnerabilities.
 *
 * @property name name of project with vulnerability
 * @property url url of project
 * @property versions versions of project
 * @property type type of link
 * @property cosvMetadata
 **/
@Entity
class CosvMetadataProject(

    var name: String,

    var url: String,

    var versions: String?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cosv_metadata_id")
    @JsonBackReference
    var cosvMetadata: CosvMetadata,

    @Enumerated(EnumType.STRING)
    var type: VulnerabilityProjectType,

) : BaseEntityWithDto<VulnerabilityProjectDto>() {
    /**
     * @return a dto
     */
    override fun toDto() = VulnerabilityProjectDto(
        name = name,
        url = url,
        versions = versions.orEmpty(),
        type = type,
        vulnerabilityIdentifier = cosvMetadata.cosvId,
    )
}
