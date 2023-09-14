package com.saveourtool.save.entities.cosv

import com.saveourtool.save.spring.entity.BaseEntity
import java.time.LocalDateTime
import javax.persistence.Entity

/**
 * Entity for COSV repository
 * @property identifier
 * @property modified
 */
@Entity
class CosvFile(
    var identifier: String,
    var modified: LocalDateTime,
) : BaseEntity() {
    override fun toString(): String = "CosvFile(identifier=$identifier, modified=$modified)"
}
