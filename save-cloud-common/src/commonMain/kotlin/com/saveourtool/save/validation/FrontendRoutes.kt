/**
 * Names that are used as endpoints in the frontend.
 * If you create a new view with new URL - add it here.
 */

package com.saveourtool.save.validation

import com.saveourtool.save.utils.URL_PATH_DELIMITER

/**
 * @property path substring of url that defines given route
 */
enum class FrontendRoutes(val path: String) {
    AWESOME_BENCHMARKS("awesome-benchmarks"),
    CONTESTS("contests"),
    CONTESTS_GLOBAL_RATING("contests/global-rating"),
    CREATE_ORGANIZATION("create-organization"),
    CREATE_PROJECT("create-project"),
    NOT_FOUND("not-found"),
    PROJECTS("projects"),
    REGISTRATION("registration"),
    SETTINGS_EMAIL("settings/email"),
    SETTINGS_ORGANIZATIONS("settings/organizations"),
    SETTINGS_PROFILE("settings/profile"),
    SETTINGS_TOKEN("settings/token"),
    ;

    companion object {
        /**
         * Get forbidden words from [FrontendRoutes].
         *
         * @return list of forbidden words
         */
        fun getForbiddenWords() = FrontendRoutes.values().map { it.path.split(URL_PATH_DELIMITER) }.flatten()
    }
}
