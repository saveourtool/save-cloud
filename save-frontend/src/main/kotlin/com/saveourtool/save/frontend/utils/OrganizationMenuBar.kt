package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.benchmarks.TabMenuBar

/**
 * A value for project menu.
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class OrganizationMenuBar(private val title: String? = null) {
    INFO,
    TOOLS,
    TESTS,
    CONTESTS,
    SETTINGS,
    ;

    /**
     * @return title or name if title is not specified
     */
    fun getTitle() = title ?: name

    companion object : TabMenuBar<OrganizationMenuBar> {
        // The string is the postfix of a [regexForUrlClassification] for parsing the url
        private val postfixInRegex = values().map { it.name.lowercase() }.joinToString { "|" }
        override val defaultTab: OrganizationMenuBar = INFO
        override val regexForUrlClassification = Regex("/project/[^/]+/[^/]+/($postfixInRegex)")
        override var paths: Pair<String, String> = "" to ""
        override fun valueOf(elem: String): OrganizationMenuBar = OrganizationMenuBar.valueOf(elem)
        override fun values(): Array<OrganizationMenuBar> = OrganizationMenuBar.values()
        override fun findEnumElement(elem: String): OrganizationMenuBar? = values().find { it.name.lowercase() == elem }
        override fun isNotAvailableWithThisRole(role: Role, elem: OrganizationMenuBar?, flag: Boolean?): Boolean = ((elem == SETTINGS) && role.isLowerThan(Role.ADMIN)) ||
                ((elem == CONTESTS) && (role.isLowerThan(Role.OWNER) || flag == false))
    }
}
