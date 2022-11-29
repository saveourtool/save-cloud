package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.diktat.DiktatDemoTool

/**
 * @property toolName
 * @property version
 * @property executableName
 * @property ownerName
 */
data class ToolKey(
    val ownerName: String,
    val toolName: String,
    val version: String,
    val executableName: String,
)

/**
 * @param executableName name of an executable - later will be fetched from database
 * @return [ToolKey] from [DiktatDemoTool]
 */
fun DiktatDemoTool.toToolKey(executableName: String) = ToolKey(
    owner,
    toolName,
    version,
    executableName,
)
