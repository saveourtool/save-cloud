package com.saveourtool.save.frontend.common.utils

/**
 * A value for project menu.
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class ProjectMenuBar {
    INFO,
    RUN,
    FILES,
    STATISTICS,
    DEMO,
    SETTINGS,
    ;
    companion object {
        val defaultTab = INFO
    }
}
