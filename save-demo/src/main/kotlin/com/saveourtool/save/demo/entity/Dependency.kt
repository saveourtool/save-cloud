package com.saveourtool.save.demo.entity

import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * @property coreTool
 * @property dependantTool
 */
@Entity
@Table(name = "dependency")
class Dependency(
    @ManyToOne
    @JoinColumn(name = "master_id")
    var coreTool: Tool,
    @ManyToOne
    @JoinColumn(name = "worker_id")
    var dependantTool: Tool,
) : BaseEntity()
