package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.Entity

/**
 * General tag [BaseEntity], that should be used for tagging different entities
 *
 * @property name tag name
 */
@Entity
class Tag(var name: String) : BaseEntity()
