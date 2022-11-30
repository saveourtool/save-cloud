package com.saveourtool.save.frontend.components.views.welcome.pagers

import com.saveourtool.save.frontend.externals.animations.*

import csstype.Width
import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.img

/**
 * Funny picture
 */
@Suppress("CUSTOM_GETTERS_SETTERS")
object BobPager : WelcomePager {
    @Suppress("MAGIC_NUMBER")
    override val animation: Animation
        get() = batch(fade(), sticky())

    override fun renderPage(childrenBuilder: ChildrenBuilder) {
        childrenBuilder.renderAnimatedPage()
    }

    private fun ChildrenBuilder.renderAnimatedPage() {
        img {
            style = jso {
                width = "60%".unsafeCast<Width>()
            }
            // FixMe: input contests here
        }
    }
}
