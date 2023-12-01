package com.saveourtool.save.cosv.frontend.components.views.organization

/**
 * A value for project menu.
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class OrganizationMenuBar(private val title: String? = null) {
    INFO,
    VULNERABILITIES,
    SETTINGS,
    ;

    /**
     * @return title or name if title is not specified
     */
    fun getTitle() = title ?: name

    companion object {
        val defaultTab: OrganizationMenuBar = INFO
    }
}
