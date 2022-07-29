/**
 * Names that are used as endpoints in the frontend.
 * If you create a new view with new URL - add it here.
 */

package com.saveourtool.save.validation

/**
 * @property path substring of url that defines given route
 */
enum class FrontendRoutes(val path: String) {
    AWESOME_BENCHMARKS("awesome-benchmarks"),
    CONTESTS("contests"),
    CREATE_ORGANIZATION("create-organization"),
    CREATE_PROJECT("create-project"),
    PROJECTS("projects"),
    SETTINGS_EMAIL("settings/email"),
    SETTINGS_ORGANIZATIONS("settings/organizations"),
    SETTINGS_PROFILE("settings/profile"),
    SETTINGS_TOKEN("settings/token"),
    ;
}
