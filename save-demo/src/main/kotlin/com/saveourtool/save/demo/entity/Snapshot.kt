package com.saveourtool.save.demo.entity

import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.Entity
import javax.persistence.Table

/**
 * @property version
 * @property executableName
 */
@Entity
@Table(name = "snapshot")
class Snapshot(
    var version: String,
    var executableName: String,
) : BaseEntity()
