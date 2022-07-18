package com.saveourtool.save.entities

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property project
 * @property contest
 */
@Entity
class LnkContestProject(
    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    @ManyToOne
    @JoinColumn(name = "contest_id")
    var contest: Contest,
) : BaseEntity() {
    /**
     * Get [ContestResult]
     *
     * @param score the best score of all LnkContestExecution by this [Project]
     * @return [ContestResult]
     */
    fun toContestResult(score: Double? = null) = ContestResult(project.name, project.organization.name, contest.name, score)
}
