package com.saveourtool.save.api.errors

/**
 * Thrown to indicate that the IP address of a host could not be determined.
 *
 * @property message the detail message.
 */
data class UnknownHostError(override val message: String) : SaveCloudError()
