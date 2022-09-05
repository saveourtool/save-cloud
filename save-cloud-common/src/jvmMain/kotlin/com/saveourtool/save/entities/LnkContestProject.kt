package com.saveourtool.save.entities

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

/**
 * @property project
 * @property contest
 * @property bestExecution
 * @property bestScore
 */
@Entity
class LnkContestProject(
    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    @ManyToOne
    @JoinColumn(name = "contest_id")
    var contest: Contest,

    @OneToOne
    @JoinColumn(name = "best_execution_id")
    var bestExecution: Execution?,

    var bestScore: Int,

) : BaseEntity() {
    /**
     * Get [ContestResult]
     *
     * @param score the best score of all LnkContestExecution by this [Project]
     * @return [ContestResult]
     */
    fun toContestResult(score: Double? = null) = ContestResult(project.name, project.organization.name, contest.name, score)
}
