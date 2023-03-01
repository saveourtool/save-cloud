package com.saveourtool.save.entities

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * Enum of contest status
 */
@Serializable
@JsExport
enum class ContestStatus {
    /**
     * Created contest
     */
    CREATED,

    /**
     * Deleted contest
     */
    DELETED,
    ;
}
