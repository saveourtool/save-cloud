package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.diktat.DiktatDemoTool

/**
 * @property ownerName name of organization that develops the tool
 * @property toolName name of a tool
 * @property vcsTagName GitHub tag that was used for file fetch (version analog)
 * @property executableName name of an executable file
 */
data class ToolKey(
    val ownerName: String,
    val toolName: String,
    val vcsTagName: String,
    val executableName: String,
) {
    /**
     * @return string that displays the tool name and GitHub release tag that was fetched
     */
    fun toPrettyString() = "$ownerName/$toolName ($vcsTagName)"
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
