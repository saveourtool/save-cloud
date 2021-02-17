package org.cqfn.save.domain

import kotlinx.serialization.Serializable

/**
 * Describes the configuration to run tests on SAVE backend.
 *
 * @property startCommand shell command to run the file
 * @property fileName name of the supplied file
 */
@Serializable
data class RunConfiguration(val startCommand: String, val fileName: String)
