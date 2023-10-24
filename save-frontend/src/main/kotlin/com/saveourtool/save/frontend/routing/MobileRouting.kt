/**
 * All routs for the mobile version of the frontend
 */

package com.saveourtool.save.frontend.routing

import com.saveourtool.save.frontend.components.mobile.AboutUsMobileView
import com.saveourtool.save.frontend.components.mobile.saveWelcomeMobileView
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.FC
import react.create
import react.react
import react.router.dom.createBrowserRouter

/**
 * Just put a map: View -> Route URL to this list
 */
val mobileRoutes = FC {
    createBrowserRouter(
        arrayOf(
            jso {
                path = FrontendRoutes.ABOUT_US.path
                element = AboutUsMobileView::class.react.create()
            },
            jso {
                path = "*"
                element = saveWelcomeMobileView.create()
            }
        )
    )
}
