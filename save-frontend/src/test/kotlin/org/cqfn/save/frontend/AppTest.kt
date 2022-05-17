package org.cqfn.save.frontend

import org.cqfn.save.frontend.externals.render
import org.cqfn.save.frontend.externals.screen
import org.w3c.dom.HTMLHeadingElement
import react.createElement
import react.react
import kotlin.js.Promise
import kotlin.test.*
import kotlin.test.Test

class AppTest {
    @Test
    fun app_should_render(): Promise<Unit> {
        render(
            createElement(App::class.react)
        )

        return screen.findByText<HTMLHeadingElement>(
            "Advanced eco-system for continuous integration, evaluation and benchmarking of software tools."
        ).then { htmlHeadingElement ->
            assertNotNull(htmlHeadingElement)
        }
    }
}
