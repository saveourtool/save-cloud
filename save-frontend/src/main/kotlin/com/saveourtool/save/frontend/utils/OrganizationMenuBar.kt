package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.TabMenuBar

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
        private val postfixInRegex = values().joinToString("|") { it.name.lowercase() }
        override val nameOfTheHeadUrlSection = "organization"
        override val defaultTab: OrganizationMenuBar = INFO
        override val regexForUrlClassification = "/$nameOfTheHeadUrlSection/[^/]+/($postfixInRegex)"
        override fun valueOf(elem: String): OrganizationMenuBar = OrganizationMenuBar.valueOf(elem)
        override fun values(): Array<OrganizationMenuBar> = OrganizationMenuBar.values()
        override fun isAvailableWithThisRole(roleName: String, elem: OrganizationMenuBar?, isOrganizationCanCreateContest: Boolean?): Boolean {
            val role = Role.valueOf(roleName)
            return !(((elem == SETTINGS) && role.isLowerThan(Role.ADMIN)) || ((elem == CONTESTS) && (role.isLowerThan(Role.OWNER) || isOrganizationCanCreateContest == false)))
        }
    }
}
