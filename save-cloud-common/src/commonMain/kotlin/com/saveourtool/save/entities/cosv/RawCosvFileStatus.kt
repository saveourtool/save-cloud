package com.saveourtool.save.entities.cosv

/**
 * Status of [RawCosvFileDto]
 */
enum class RawCosvFileStatus {
    /**
     * A raw cosv file only uploaded to the platform
     */
    UPLOADED,

    /**
     * A raw cosv file processed by the platform
     */
    PROCESSED,

    /**
     * The platform failed to process a raw cosv file
     */
    FAILED,
}
