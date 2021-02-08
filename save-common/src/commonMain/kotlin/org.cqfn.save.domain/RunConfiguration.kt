package org.cqfn.save.domain

/**
 * Describes the configuration to run tests on SAVE backend.
 *
 * @property startCommand shell command to run the file
 * @property fileName name of the supplied file
 */
data class RunConfiguration(val startCommand: String, val fileName: String)
