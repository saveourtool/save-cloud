package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.benchmarks.MenuBar

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

    companion object : MenuBar<OrganizationMenuBar> {
        override fun valueOf(): OrganizationMenuBar = OrganizationMenuBar.valueOf()
        override fun values(): Array<OrganizationMenuBar> = OrganizationMenuBar.values()
        override val defaultTab: OrganizationMenuBar = INFO
        val listOfStringEnumElements = OrganizationMenuBar.values().map { it.name.lowercase() }
        override val regex = Regex("/project/[^/]+/[^/]+/[^/]+")
        override fun findEnumElements(elem: String): OrganizationMenuBar? = values().find { it.name.lowercase() == elem }

        override var paths: Pair<String, String> = "" to ""
        override fun setPath(shortPath: String, longPath: String) {
            paths = shortPath to longPath
        }

        override fun returnStringOneOfElements(elem: OrganizationMenuBar): String = elem.name

        override fun isAvailableWithThisRole(role: Role, elem: OrganizationMenuBar?, flag: Boolean?): Boolean = ((elem == SETTINGS) && !role.isHigherOrEqualThan(Role.ADMIN)) ||
                ((elem == CONTESTS) && (!role.isHigherOrEqualThan(Role.OWNER) || flag == false))
    }
}
