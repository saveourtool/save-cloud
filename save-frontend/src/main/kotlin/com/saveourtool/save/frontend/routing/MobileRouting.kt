/**
 * All routs for the mobile version of the frontend
 */

package com.saveourtool.save.frontend.routing

import com.saveourtool.save.frontend.components.mobile.AboutUsMobileView
import com.saveourtool.save.frontend.components.mobile.saveWelcomeMobileView
import com.saveourtool.save.validation.FrontendRoutes
import react.FC
import react.create
import react.react
import react.router.PathRoute
import react.router.Routes

/**
 * Just put a map: View -> Route URL to this list
 */
val mobileRoutes: FC<Props> = FC {
    Routes {
        listOf(
            AboutUsMobileView::class.react.create() to FrontendRoutes.ABOUT_US,
            saveWelcomeMobileView.create() to "*",
        ).forEach {
            PathRoute {
                this.element = it.first
                this.path = "/${it.second}"
            }
        }
    }
}
