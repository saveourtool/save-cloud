package com.saveourtool.save.frontend.utils

/**
 * A value for project menu.
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class ProjectMenuBar {
    INFO,
    RUN,
    STATISTICS,
    SETTINGS,
    ;

    companion object  {
        val defaultTab: ProjectMenuBar = INFO

        val listOfStringEnumElements = ProjectMenuBar.values().map { it.name.lowercase() }
    }
}
