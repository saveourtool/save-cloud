package com.saveourtool.save.frontend

import com.saveourtool.save.frontend.externals.findByTextAndCast
import com.saveourtool.save.frontend.externals.render
import com.saveourtool.save.frontend.externals.screen
import com.saveourtool.save.frontend.routing.basicRouting
import web.html.HTMLHeadingElement
import react.create
import react.router.MemoryRouter
import kotlin.js.Promise
import kotlin.test.*
import kotlin.test.Test

class BasicRoutingTest {
    @Test
    fun basicRoutingShouldRenderIndexViewTest(): Promise<Unit> {
        // App uses `BrowserRouter`, while `MemoryRouter` should be used for tests. Thus, app cannot be rendered
        render(
            MemoryRouter.create {
                basicRouting()
            }
        )

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
