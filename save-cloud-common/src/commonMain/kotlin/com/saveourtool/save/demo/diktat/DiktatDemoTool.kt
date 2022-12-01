package com.saveourtool.save.demo.diktat

/**
 * Enum that represents possible tool names in diktat-demo
 *
 * @property ownerName name of organization that develops the tool
 * @property toolName name of a tool
 * @property vcsTagName GitHub tag that was used for file fetch (version analog)
 */
enum class DiktatDemoTool(val ownerName: String, val toolName: String, val vcsTagName: String) {
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
