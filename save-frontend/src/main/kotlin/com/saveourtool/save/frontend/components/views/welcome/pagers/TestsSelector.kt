package com.saveourtool.save.frontend.components.views.welcome.pagers

import com.saveourtool.save.frontend.externals.animations.*

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.img
import web.cssom.Height
import web.cssom.Width

@Suppress("CUSTOM_GETTERS_SETTERS", "MAGIC_NUMBER")
object TestsSelector : WelcomePager {
    override val animation: Animation
        get() = batch(fade(), sticky())

    override fun renderPage(childrenBuilder: ChildrenBuilder) {
        childrenBuilder.renderAnimatedPage()
    }

    private fun ChildrenBuilder.renderAnimatedPage() {
        img {
            style = jso {
                width = "100%".unsafeCast<Width>()
                height = "auto".unsafeCast<Height>()
            }
            src = "img/tests_selector.png"
        }
    }
}
