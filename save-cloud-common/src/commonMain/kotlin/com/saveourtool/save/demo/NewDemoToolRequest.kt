package com.saveourtool.save.demo

/**
 * @property organizationName name of an organization on GitHub that maintains the tool
 * @property projectName name of repo on GitHub
 * @property vcsTagName release tag that defines the version to be fetched
 */
data class NewDemoToolRequest(
    val organizationName: String,
    val projectName: String,
    val vcsTagName: String,
)
