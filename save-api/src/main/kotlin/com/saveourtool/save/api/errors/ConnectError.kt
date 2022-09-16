package com.saveourtool.save.api.errors

/**
 * An error occurred while attempting to connect a socket to a remote address
 * and port.
 *
 * @property message the detail message.
 */
data class ConnectError(override val message: String) : SaveCloudError()
