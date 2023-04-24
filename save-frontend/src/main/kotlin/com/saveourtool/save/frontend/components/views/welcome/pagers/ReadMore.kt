/**
 * [Last page] Main information about SAVE-cloud
 */

package com.saveourtool.save.frontend.components.views.welcome.pagers

import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h3
import web.cssom.AlignItems
import web.cssom.AlignSelf
import web.cssom.ClassName
import web.cssom.Color
import web.cssom.Display
import web.cssom.JustifyContent
import web.cssom.TextAlign
import web.cssom.em

/**
 * rendering of "Read more" section
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.renderReadMorePage() {
    div {
        className = ClassName("col")
        style = jso {
            bottom = 30.em
        }

        wantToKnowMore()

        div {
            className = ClassName("col justify-content-center")
            div {
                className = ClassName("row justify-content-center")
                link("https://github.com/saveourtool/save-cloud", "Github")
            }
            div {
                className = ClassName("row justify-content-center")
                link("https://github.com/saveourtool/save-cloud/blob/master/info/SaveMotivation.md", "Motivation")
            }
            div {
                className = ClassName("row justify-content-center")
                link("#/about", "About us")
            }
        }
    }
}

private fun ChildrenBuilder.wantToKnowMore() {
    div {
        className = ClassName("col justify-content-center")
        h1 {
            style = jso {
                textAlign = TextAlign.center
                color = "rgb(6, 7, 89)".unsafeCast<Color>()
            }
            +"Want to know more about SAVE?"
        }
    }
}

private fun ChildrenBuilder.link(url: String, text: String) {
    div {
        className = ClassName("col-2")
        style = jso {
            justifyContent = JustifyContent.center
            display = Display.flex
            alignItems = AlignItems.center
            alignSelf = AlignSelf.center
        }
        a {
            href = url
            h3 {
                className = ClassName("text-center")
                +text
            }
        }
    }
}
