package com.saveourtool.save.permission

import com.saveourtool.save.domain.Role

import kotlinx.serialization.Serializable

/**
 * @property organizationName organization that will gain or loose rights
 * @property rights new rights
 */
@Serializable
data class SetRightsRequest(
    val organizationName: String,
    val rights: Rights,
)
