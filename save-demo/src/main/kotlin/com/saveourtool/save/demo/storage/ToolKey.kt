package com.saveourtool.save.demo.storage

/**
 * @property toolName
 * @property version
 * @property executableName
 */
data class ToolKey(
    val toolName: String,
    val version: String,
    val executableName: String,
)
