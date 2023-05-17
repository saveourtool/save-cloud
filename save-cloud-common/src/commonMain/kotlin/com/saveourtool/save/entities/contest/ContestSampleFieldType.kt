package com.saveourtool.save.entities.contest

import kotlinx.serialization.Serializable

/**
 * Enum of contest sample field type
 */
@Serializable
enum class ContestSampleFieldType {
    /**
     * Multi string type
     */
    MULTI_STRING,

    /**
     * Number type
     */
    NUM,

    /**
     * Single string type
     */
    SINGLE_STRING,
    ;
}
