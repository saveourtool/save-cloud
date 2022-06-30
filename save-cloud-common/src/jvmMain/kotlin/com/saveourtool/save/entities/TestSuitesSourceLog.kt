package com.saveourtool.save.entities

import javax.persistence.Entity
import javax.persistence.ManyToOne

/**
 * @property source
 * @property version
 */
@Entity
class TestSuitesSourceLog(
    @ManyToOne(optional = false)
    @JoinColumn("source_id")
    var source: TestSuitesSource,
    var version: String,
) : BaseEntity()
