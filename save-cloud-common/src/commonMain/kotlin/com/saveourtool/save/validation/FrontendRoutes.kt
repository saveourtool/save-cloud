/**
 * Names that are used as endpoints in the frontend.
 * If you create a new view with new URL - add it here.
 */

package com.saveourtool.save.validation

enum class FrontendRoutes(val path: String) {
    AWESOME_BENCHMARKS("awesome-benchmarks"),
    CONTESTS("contests"),
    CREATE_PROJECT("create-project"),
    CREATE_ORGANIZATION("create-organization"),
    PROJECTS("projects"),
    SETTINGS_EMAIL("settings/email"),
    SETTINGS_ORGANIZATIONS("settings/organizations"),
    SETTINGS_PROFILE("settings/profile"),
    SETTINGS_TOKEN("settings/token"),
    ;
}