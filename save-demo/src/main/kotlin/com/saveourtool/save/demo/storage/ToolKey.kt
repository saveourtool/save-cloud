package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.demo.entity.Dependency

/**
 * @property organizationName name of organization that develops the tool
 * @property projectName name of a tool
 * @property version GitHub tag that was used for file fetch (version analog)
 * @property fileName name of an executable file
 */
data class ToolKey(
    val organizationName: String,
    val projectName: String,
    val version: String,
    val fileName: String,
) {
    /**
     * @return string that displays the tool name and GitHub release tag that was fetched
     */
    fun toPrettyString() = "$organizationName/$projectName ($version)"
}

/**
 * @param executableName name of an executable - later will be fetched from database
 * @return [ToolKey] from [DiktatDemoTool]
 */
fun DiktatDemoTool.toToolKey(executableName: String) = ToolKey(
    ownerName,
    toolName,
    vcsTagName,
    executableName,
)

/**
 * @return [ToolKey] from [Dependency]
 */
fun Dependency.toToolKey(): ToolKey = ToolKey(
    organizationName = demo.organizationName,
    projectName = demo.projectName,
    version = version,
    fileName = fileName,
)
