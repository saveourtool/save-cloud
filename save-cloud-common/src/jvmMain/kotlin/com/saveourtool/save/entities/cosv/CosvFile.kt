package com.saveourtool.save.entities.cosv

import com.saveourtool.save.spring.entity.BaseEntity
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

/**
 * Entity for COSV repository
 * @property identifier
 * @property modified
 * @property prevCosvFile
 * @property vulnerabilityMetadata
 */
@Entity
class CosvFile(
    var identifier: String,
    var modified: LocalDateTime,
    @OneToOne
    @JoinColumn(name = "prev_cosv_file_id")
    var prevCosvFile: CosvFile? = null,
    @ManyToOne
    @JoinColumn(name = "vulnerability_metadata_id")
    var vulnerabilityMetadata: VulnerabilityMetadata? = null,
) : BaseEntity() {
    override fun toString(): String = "CosvFile(identifier=$identifier, modified=$modified)"
}
