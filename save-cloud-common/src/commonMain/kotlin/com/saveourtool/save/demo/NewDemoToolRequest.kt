package com.saveourtool.save.demo

/**
 * @property organizationName
 * @property projectName
 * @property vcsTagName
 */
data class NewDemoToolRequest(
    val organizationName: String,
    val projectName: String,
    val vcsTagName: String,
)
