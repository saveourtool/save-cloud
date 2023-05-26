package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.TabMenuBar

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
    SECURITY,
    ;

    companion object : TabMenuBar<ProjectMenuBar> {
        // The string is the postfix of a [regexForUrlClassification] for parsing the url
        private val postfixInRegex = values().joinToString("|") { it.name.lowercase() }
        override val nameOfTheHeadUrlSection = "project"
        override val defaultTab: ProjectMenuBar = INFO
        override val regexForUrlClassification = "/$nameOfTheHeadUrlSection/[^/]+/[^/]+/($postfixInRegex)"
        override fun valueOf(elem: String): ProjectMenuBar = ProjectMenuBar.valueOf(elem)
        override fun values(): Array<ProjectMenuBar> = ProjectMenuBar.values()
        override fun isAvailableWithThisRole(role: Role, elem: ProjectMenuBar?, isOrganizationCanCreateContest: Boolean?): Boolean =
                !(((elem == SETTINGS) || (elem == RUN)) && role.isLowerThan(Role.ADMIN))
    }
}
