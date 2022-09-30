package com.saveourtool.save.sandbox.entity

import com.saveourtool.save.entities.BaseEntity
import javax.persistence.Entity

@Entity(name = "save_cloud.user")
class SandboxUser(
    var name: String,
) : BaseEntity()