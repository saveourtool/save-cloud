package com.saveourtool.save.entities

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

/**
 * Entity that represents a link between [Contest] and [Execution]
 *
 * @property contest
 * @property execution
 * @property score the result of an execution
 */
@Entity
class LnkContestExecution(
    @OneToOne
    @JoinColumn(name = "execution_id")
    var execution: Execution,

    @ManyToOne
    @JoinColumn(name = "contest_id")
    var contest: Contest,

    var score: Double
) : BaseEntity()
