package com.saveourtool.save.frontend

import com.saveourtool.save.frontend.externals.findByTextAndCast
import com.saveourtool.save.frontend.externals.render
import com.saveourtool.save.frontend.externals.screen
import web.html.HTMLHeadingElement
import react.create
import react.react
import kotlin.js.Promise
import kotlin.test.*
import kotlin.test.Test

class AppTest {
    @Test
    fun appShouldRender(): Promise<Unit> {
        render(
            App::class.react.create()
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
