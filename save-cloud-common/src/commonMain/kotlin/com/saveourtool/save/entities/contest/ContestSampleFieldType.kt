package com.saveourtool.save.entities.contest

import kotlinx.serialization.Serializable

/**
 * Enum of contest sample field type
 */
@Serializable
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class ContestSampleFieldType {
    /**
     * Number type
     */
    NUM,

    /**
     * Single string type
     */
    SINGLE_STRING,

    /**
     * Multi string type
     */
    MULTI_STRING,
    ;
}
