/**
 * Names that are used as endpoints in the frontend.
 * If you create a new view with new URL - add it here.
 */

package com.saveourtool.save.validation

import com.saveourtool.save.utils.URL_PATH_DELIMITER
import kotlin.js.JsExport

/**
 * @property path substring of url that defines given route
 */
@JsExport
enum class FrontendRoutes(val path: String) {
    ABOUT_US("about"),
    AWESOME_BENCHMARKS("awesome-benchmarks"),
    CONTESTS("contests"),
    CONTESTS_GLOBAL_RATING("contests/global-rating"),
    CREATE_ORGANIZATION("create-organization"),
    CREATE_PROJECT("create-project"),
    CREATE_VULNERABILITY("foss-graph/create-vulnerability"),
    DEMO("demo"),
    FOSS_GRAPH("foss-graph"),
    MANAGE_ORGANIZATIONS("organizations"),
    NOT_FOUND("not-found"),
    PROJECTS("projects"),
    REGISTRATION("registration"),
    SANDBOX("sandbox"),
    SETTINGS_EMAIL("settings/email"),
    SETTINGS_ORGANIZATIONS("settings/organizations"),
    SETTINGS_PROFILE("settings/profile"),
    SETTINGS_TOKEN("settings/token"),
    ;

    override fun toString(): String = path

    companion object {
        /**
         * Get forbidden words from [FrontendRoutes].
         *
         * @return list of forbidden words
         */
        fun getForbiddenWords() = FrontendRoutes.values().map { it.path.split(URL_PATH_DELIMITER) }.flatten().toTypedArray()
    }
}
