package com.saveourtool.save.demo

import kotlinx.serialization.Serializable

/**
 * @property organizationName name of an organization on GitHub that maintains the tool
 * @property projectName name of repo on GitHub
 * @property vcsTagName release tag that defines the version to be fetched
 * @property runCommand
 * @property fileName
 */
@Serializable
data class NewDemoToolRequest(
    val organizationName: String,
    val projectName: String,
    val vcsTagName: String,
    val runCommand: String,
    val fileName: String,
) {
    companion object {
        val empty = NewDemoToolRequest(
            "",
            "",
            "",
            "",
            "",
        )
    }
}
