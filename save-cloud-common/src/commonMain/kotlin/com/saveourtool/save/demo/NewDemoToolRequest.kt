package com.saveourtool.save.demo

import kotlinx.serialization.Serializable

/**
 * @property ownerName name of user/organization on GitHub that maintains the tool repo
 * @property repoName name of repo on GitHub
 * @property vcsTagName release tag that defines the version to be fetched
 * @property runCommand
 * @property fileName
 */
@Serializable
data class NewDemoToolRequest(
    val ownerName: String,
    val repoName: String,
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
