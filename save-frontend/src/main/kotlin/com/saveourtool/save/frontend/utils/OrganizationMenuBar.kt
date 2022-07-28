package com.saveourtool.save.frontend.utils

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
}
