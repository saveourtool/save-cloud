package org.cqfn.save.frontend

import kotlinx.js.jso
import org.cqfn.save.frontend.components.topBar
import org.cqfn.save.frontend.externals.render
import org.cqfn.save.frontend.externals.screen
import org.w3c.dom.HTMLDivElement
import react.ReactNode
import react.buildElement
import react.createElement
import react.dom.html.ReactHTML
import kotlin.test.*
import kotlin.test.Test

//import react.React

class Test {
    @Test
    fun test() {
        val rr = render(
            createElement(type = ReactHTML.div, children = arrayOf(ReactNode("stuff"))),
            undefined
        )
        val myDiv = screen.getByText("stuff") as HTMLDivElement?
        assertNotNull(myDiv)
    }
}
