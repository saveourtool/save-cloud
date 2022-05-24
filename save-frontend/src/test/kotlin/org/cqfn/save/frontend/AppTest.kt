package com.saveourtool.save.frontend

import com.saveourtool.save.frontend.externals.findByTextAndCast
import com.saveourtool.save.frontend.externals.render
import com.saveourtool.save.frontend.externals.screen
import org.w3c.dom.HTMLHeadingElement
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

        return screen.findByTextAndCast<HTMLHeadingElement>(
            "Advanced eco-system for continuous integration, evaluation and benchmarking of software tools."
        ).then { htmlHeadingElement ->
            assertNotNull(htmlHeadingElement)
        }
    }
}
