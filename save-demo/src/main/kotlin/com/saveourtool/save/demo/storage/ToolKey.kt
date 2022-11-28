package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.diktat.DiktatDemoTool

/**
 * @property toolName
 * @property version
 * @property executableName
 */
data class ToolKey(
    val ownerName: String,
    val toolName: String,
    val version: String,
    val executableName: String,
)

fun DiktatDemoTool.toToolKey(executableName: String) = ToolKey(
    owner,
    toolName,
    version,
    executableName,
)