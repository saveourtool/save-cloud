package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.Tag
import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property cosvMetadata [CosvMetadata]
 * @property tag [Tag] associated with [cosvMetadata]
 */
@Entity
class LnkCosvMetadataTag(
    @ManyToOne
    @JoinColumn(name = "cosv_metadata_id")
    var cosvMetadata: CosvMetadata,

    @ManyToOne
    @JoinColumn(name = "tag_id")
    var tag: Tag,
) : BaseEntity()
