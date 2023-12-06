package com.saveourtool.save.frontend.common.components.views.organization

/**
 * A value for organization menu.
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class OrganizationMenuBar(private val title: String? = null) {
    INFO,
    VULNERABILITIES,
    TOOLS,
    BENCHMARKS,
    CONTESTS,
    SETTINGS,
    ;

    /**
     * @return title or name if title is not specified
     */
    fun getTitle() = title ?: name

    companion object {
        val defaultTab: OrganizationMenuBar = INFO
        val cosvTabs: Array<OrganizationMenuBar> = arrayOf(INFO, VULNERABILITIES, SETTINGS)
        val saveTabs: Array<OrganizationMenuBar> = arrayOf(INFO, TOOLS, BENCHMARKS, CONTESTS, SETTINGS)
    }
}
