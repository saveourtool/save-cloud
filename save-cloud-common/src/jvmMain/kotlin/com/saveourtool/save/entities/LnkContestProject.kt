package com.saveourtool.save.entities

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property project
 * @property contest
 * @property score score of [project] in [contest]
 */
@Entity
class LnkContestProject(
    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    @ManyToOne
    @JoinColumn(name = "contest_id")
    var contest: Contest,

    var score: Float,
) : BaseEntity()
