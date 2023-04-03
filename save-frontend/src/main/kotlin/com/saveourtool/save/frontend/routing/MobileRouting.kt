/**
 * All routs for the mobile version of the frontend
 */

package com.saveourtool.save.frontend.routing

import com.saveourtool.save.frontend.components.mobile.AboutUsMobileView
import com.saveourtool.save.frontend.components.mobile.WelcomeMobileView
import com.saveourtool.save.validation.FrontendRoutes
import react.VFC
import react.create
import react.react
import react.router.Route
import react.router.Routes

/**
 * Just put a map: View -> Route URL to this list
 */
val mobileRoutes = VFC {
    Routes {
        listOf(
            AboutUsMobileView::class.react.create() to FrontendRoutes.ABOUT_US.path,
            WelcomeMobileView::class.react.create() to "*",
        ).forEach {
            Route {
                this.element = it.first
                this.path = "/${it.second}"
            }
        }
    }
}
