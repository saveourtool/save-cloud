package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.Entity
import javax.persistence.Table

/**
 * General tag [BaseEntity], that should be used for tagging different entities
 *
 * @property name tag name
 */
@Entity
@Table(schema = "save_cloud", name = "tag")
class Tag(var name: String) : BaseEntity()
