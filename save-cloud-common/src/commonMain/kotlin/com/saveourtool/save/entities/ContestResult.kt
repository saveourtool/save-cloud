package com.saveourtool.save.entities

import kotlinx.serialization.Serializable

/**
 * Represents all data related to contest result
 * @property projectName name of a project
 * @property organizationName name of an organization in which given project is
 * @property score that project got in contest
 */
@Serializable
data class ContestResult(
    val projectName: String,
    val organizationName: String,
    val contestName: String,
    val score: Float,
)
