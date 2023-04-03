package com.saveourtool.save.api.errors

import com.saveourtool.save.api.SaveCloudClientEx

/**
 * A top-level class for errors returned by [SaveCloudClientEx] calls.
 *
 * @see SaveCloudClientEx
 */
sealed class SaveCloudError {
    /**
     * The detail message.
     */
    abstract val message: String
}
