package com.saveourtool.save.info

import kotlinx.serialization.Serializable

/**
 * @property isPermittedCreateContest permission for create contests in organizations
 * @property isPermittedToBulkUpload permission for upload COSV files in organizations
 */
@Serializable
data class UserPermissions(
    val isPermittedCreateContest: Map<String, Boolean> = emptyMap(),
    val isPermittedToBulkUpload: Map<String, Boolean> = emptyMap(),
)
