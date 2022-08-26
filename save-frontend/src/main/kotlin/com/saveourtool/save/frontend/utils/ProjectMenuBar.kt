package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.benchmarks.TabMenuBar

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

    companion object : TabMenuBar<ProjectMenuBar> {
        // The string is the postfix of a [regexForUrlClassification] for parsing the url
        private val postfixInRegex = values().map { it.name.lowercase() }.joinToString { "|" }
        override val defaultTab: ProjectMenuBar = INFO
        val listOfStringEnumElements = ProjectMenuBar.values().map { it.name.lowercase() }
        override val regexForUrlClassification = Regex("/project/[^/]+/[^/]+/($postfixInRegex)")
        override var paths: Pair<String, String> = "" to ""
        override fun valueOf(elem: String): ProjectMenuBar = ProjectMenuBar.valueOf(elem)
        override fun values(): Array<ProjectMenuBar> = ProjectMenuBar.values()
        override fun findEnumElement(elem: String): ProjectMenuBar? = values().find { it.name.lowercase() == elem }
        override fun convertEnumElemToString(elem: ProjectMenuBar): String = elem.name

        override fun isNotAvailableWithThisRole(role: Role, elem: ProjectMenuBar?, flag: Boolean?): Boolean = ((elem == SETTINGS) || (elem == RUN)) && role.isLowerThan(Role.ADMIN)
    }
}
