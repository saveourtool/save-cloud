/**
 * Names that are used as endpoints in the frontend.
 * If you create a new view with new URL - add it here.
 */

package com.saveourtool.save.validation

import com.saveourtool.save.utils.Constants.URL_PATH_DELIMITER
import kotlin.js.JsExport

/**
 * @property path substring of url that defines given route
 */
@JsExport
sealed class FrontendRoutes(val path: String) {

    init {
        lazyValues += lazy { this }
    }

    override fun toString(): String = path

    companion object {
        private val lazyValues = mutableListOf<Lazy<FrontendRoutes>>()

        private val values by lazy {
            lazyValues.map { it.value }
        }

        /**
         * Get forbidden words from [FrontendRoutes].
         *
         * @return list of forbidden words
         */
        fun getForbiddenWords(): Array<String> = values.map { it.path.split(URL_PATH_DELIMITER) }.flatten().toTypedArray()
    }

    object ABOUT_US : FrontendRoutes("about")
    object AWESOME_BENCHMARKS : FrontendRoutes("awesome-benchmarks")
    object CONTESTS : FrontendRoutes("contests")
    object CONTESTS_GLOBAL_RATING : FrontendRoutes("contests/global-rating")
    object CREATE_ORGANIZATION : FrontendRoutes("create-organization")
    object CREATE_PROJECT : FrontendRoutes("create-project")
    object DEMO : FrontendRoutes("demo")
    object FOSS_GRAPH : FrontendRoutes("foss-graph")
    object MANAGE_ORGANIZATIONS : FrontendRoutes("organizations")
    object NOT_FOUND : FrontendRoutes("not-found")
    object PROJECTS : FrontendRoutes("projects")
    object REGISTRATION : FrontendRoutes("registration")
    object SANDBOX : FrontendRoutes("sandbox")
    object SETTINGS_EMAIL : FrontendRoutes("settings/email")
    object SETTINGS_ORGANIZATIONS : FrontendRoutes("settings/organizations")
    object SETTINGS_PROFILE : FrontendRoutes("settings/profile")
    object SETTINGS_TOKEN : FrontendRoutes("settings/token")
}
