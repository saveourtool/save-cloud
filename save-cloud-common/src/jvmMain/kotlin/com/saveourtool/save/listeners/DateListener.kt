package com.saveourtool.save.listeners

import com.saveourtool.save.entities.vulnerabilities.Vulnerability
import java.time.LocalDateTime
import javax.persistence.PrePersist
import javax.persistence.PreUpdate

@Suppress("MISSING_KDOC_TOP_LEVEL")
class DateListener {
    @PrePersist
    private fun beforeSave(entity: Vulnerability) {
        with(entity) {
            createDate = LocalDateTime.now()
            updateDate = LocalDateTime.now()
        }
    }
    @PreUpdate
    private fun beforeUpdate(entity: Vulnerability) {
        with(entity) {
            updateDate = LocalDateTime.now()
        }
    }
}
