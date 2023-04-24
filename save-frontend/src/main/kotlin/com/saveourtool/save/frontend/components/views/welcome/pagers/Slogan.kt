/**
 * Slogans that will be used in welcome page
 */

package com.saveourtool.save.frontend.components.views.welcome.pagers

import com.saveourtool.save.frontend.externals.animations.Animation
import com.saveourtool.save.frontend.externals.animations.zoomInScrollOut

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.h1
import web.cssom.Color
import web.cssom.FontSize
import web.cssom.TextAlign

/**
 * Class for a highlighted slogans
 */
@Suppress("CUSTOM_GETTERS_SETTERS")
open class Slogan(
    private val text: String,
    private val linkUrl: String = "",
    private val linkText: String = ""
) : WelcomePager {
    override val animation: Animation
        get() = zoomInScrollOut

    override fun renderPage(childrenBuilder: ChildrenBuilder) {
        childrenBuilder.renderAnimatedPage()
    }

    private fun ChildrenBuilder.renderAnimatedPage() {
        h1 {
            style = jso {
                color = "rgb(6, 7, 89)".unsafeCast<Color>()
                textAlign = TextAlign.center
                fontSize = "3.0rem".unsafeCast<FontSize>()
            }
            +text
        }
        if (linkUrl.isNotEmpty()) {
            a {
                href = linkUrl
                h1 {
                    style = jso {
                        textAlign = TextAlign.center
                        fontSize = "3.0rem".unsafeCast<FontSize>()
                    }
                    +linkText
                }
            }
        }
    }
}

object SloganAboutCi : Slogan("Cloud CI platform with a main focus on code analyzers")
object SloganAboutTests : Slogan("Share your tests with the community")
object SloganAboutBenchmarks : Slogan("Archive with popular community benchmarks")
object SloganAboutContests : Slogan("Participate in Community", "#/contests", "Contests")
