package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.benchmarks.MenuBar


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

    companion object : MenuBar<ProjectMenuBar> {

        override fun valueOf(): ProjectMenuBar = ProjectMenuBar.valueOf()
        override fun values(): Array<ProjectMenuBar> = ProjectMenuBar.values()
        override val defaultTab: ProjectMenuBar = INFO
        val listOfStringEnumElements = ProjectMenuBar.values().map { it.name.lowercase() }
        override val regex = Regex("/project/[^/]+/[^/]+/[^/]+")
        override fun findEnumElements(elem: String): ProjectMenuBar? = values().find { it.name.lowercase() == elem }

        override var paths: Pair<String, String> = "" to ""
        override fun setPath(shortPath: String, longPath: String) {
            paths = shortPath to longPath
        }

        override fun returnStringOneOfElements(elem: ProjectMenuBar): String = elem.name

        override fun isAvailableWithThisRole(role: Role, elem: ProjectMenuBar?, flag: Boolean?): Boolean = ((elem == SETTINGS) || (elem == RUN)) &&
                !role.isHigherOrEqualThan(Role.ADMIN)
    }
}
