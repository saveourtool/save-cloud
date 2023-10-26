package com.saveourtool.save.frontend

import com.saveourtool.save.frontend.externals.findByTextAndCast
import com.saveourtool.save.frontend.externals.i18next.initI18n
import com.saveourtool.save.frontend.externals.render
import com.saveourtool.save.frontend.externals.screen
import com.saveourtool.save.frontend.routing.createBasicRoutes
import com.saveourtool.save.info.UserInfo
import js.core.jso
import react.FC
import react.create
import web.html.HTMLHeadingElement
import react.router.createMemoryRouter
import react.router.dom.RouterProvider
import react.useState
import kotlin.js.Promise
import kotlin.test.*
import kotlin.test.Test

class BasicRoutingTest {
    @Test
    fun basicRoutingShouldRenderIndexViewTest(): Promise<Unit> {
        val (userInfo, setUserInfo) = useState<UserInfo>()
        // App uses `BrowserRouter`, while `MemoryRouter` should be used for tests. Thus, app cannot be rendered
        val routerProvider = FC {
            RouterProvider {
                router = createMemoryRouter(
                    arrayOf(jso {
                        path = "/"
                        children = createBasicRoutes(userInfo, setUserInfo)
                    })
                )
                initI18n()
            }
        }
        render(routerProvider.create())

        screen.findByTextAndCast<HTMLHeadingElement>(
            "Cloud Platform for CI and Benchmarking of Code Analyzers"
        ).then { htmlHeadingElement ->
            assertNotNull(htmlHeadingElement)
        }

        return screen.findByTextAndCast<HTMLHeadingElement>(
            "Archive of 1-Day Vulnerabilities Aggregated from Various Sources"
        ).then { htmlHeadingElement ->
            assertNotNull(htmlHeadingElement)
        }
    }
}
