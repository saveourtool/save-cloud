package com.saveourtool.save.info

import kotlinx.serialization.Serializable

/**
 * @property isPermittedCreateContest permission for create contests
 * @property isPermittedToBulkUpload permission for upload COSV files
 * @property organizationName name of organization
 */
@Serializable
data class UserPermissions(
    val isPermittedCreateContest: Boolean = false,
    val isPermittedToBulkUpload: Boolean = false,
    val organizationName: String? = null,
)
