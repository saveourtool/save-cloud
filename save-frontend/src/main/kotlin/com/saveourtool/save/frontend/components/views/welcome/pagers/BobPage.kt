package com.saveourtool.save.frontend.components.views.welcome.pagers

import com.saveourtool.save.frontend.externals.animations.*

import csstype.Width
import react.ChildrenBuilder
import react.dom.html.ReactHTML

import kotlinx.js.jso

/**
 * Funny picture
 */
@Suppress("CUSTOM_GETTERS_SETTERS")
object BobPager : WelcomePager {
    @Suppress("MAGIC_NUMBER")
    override val animation: Animation
        get() = batch(fade(), sticky(45))

    override fun renderPage(childrenBuilder: ChildrenBuilder) {
        childrenBuilder.renderAnimatedPage()
    }

    private fun ChildrenBuilder.renderAnimatedPage() {
        ReactHTML.img {
            style = jso {
                width = "140%".unsafeCast<Width>()
            }
            src = "img/bob.png"
        }
    }
}
