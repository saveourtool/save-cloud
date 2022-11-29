package com.saveourtool.save.demo.diktat

/**
 * Enum that represents possible tool names in diktat-demo
 * @property owner
 * @property toolName
 * @property version
 */
enum class DiktatDemoTool(val owner: String, val toolName: String, val version: String) {
    /**
     * Run diktat
     */
    DIKTAT("saveourtool", "diktat", "v1.2.3"),

    /**
     * Run ktlint
     */
    KTLINT("pinterest", "ktlint", "0.46.1"),
    ;
}
