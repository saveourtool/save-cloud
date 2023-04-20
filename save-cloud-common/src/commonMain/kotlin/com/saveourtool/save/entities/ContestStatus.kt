package com.saveourtool.save.entities

import kotlin.js.JsExport
import kotlinx.serialization.Serializable

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
