@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.validation.FrontendRoutes
import com.saveourtool.save.validation.SETTINGS
import remix.run.router.Location

/**
 * @param url url for comparison
 * @return true if [Location.pathname] is not [url], false otherwise
 */
fun Location.not(url: String) = pathname != url

/**
 * @param urls list of urls
 * @return true of [Location] is not in [urls]
 */
fun Location.notIn(urls: List<String>) = urls.all { not(it) }

/**
 * @param routes array of [FrontendRoutes]
 * @return true of [Location] is not in [FrontendRoutes.path] of [routes]
 */
fun Location.notIn(routes: Array<FrontendRoutes>) = notIn(routes.map { "/$it" })

/**
 * @return true if [Location.pathname] starts with `/vuln`, false otherwise
 */
fun Location.isVuln() = this.pathname.startsWith("/vuln")

/**
 * @return true if [Location.pathname] starts with `/settings`, false otherwise
 */
fun Location.isSettings() = this.pathname.startsWith("/$SETTINGS")

/**
 * @return true if [Location.pathname] matches organization URL: /org_name
 *
 */
fun Location.isOrganization(): Boolean {
    // matches all, starting from first `/` and before `?` or second `/`
    val isPathFollowedFromFirstSlash = ("^/[^?/]*").toRegex().matches(this.pathname)
    // not in basic frontend routes
    val isNotInFrontendRoutes = this.pathname.drop(1) !in FrontendRoutes.values().map { it.path }

    return isPathFollowedFromFirstSlash && isNotInFrontendRoutes
}

/**
 * @return true if we are on a main (index) page
 */
fun Location.isIndex() = pathname == "/"
