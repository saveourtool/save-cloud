/**
 * Main view for Demos
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.components.views.welcome.marketingTitle
import com.saveourtool.save.frontend.utils.*
import js.core.jso
import react.ChildrenBuilder

import react.VFC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.main
import web.cssom.*

val indexView: VFC = VFC {
    useBackground(Style.BLUE)

    main {
        className = ClassName("main-content")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("grid")
                div {
                    className = ClassName("row")
                    creationCard("/", "img/save-logo-bg.jpg", "", JustifyContent.flexEnd)
                    creationCard("/", "img/vuln-logo-bg.jpg", "", JustifyContent.flexStart)
                }

                div {
                    className = ClassName("row")
                    creationCard("/", "", "SAVE", JustifyContent.center)
                    creationCard("/", "", "Vulnerabilities", JustifyContent.center)
                }
            }
        }
    }
}

private fun ChildrenBuilder.creationCard(url: String, img: String, text: String, justifyContentValue: JustifyContent) {
    div {
        className = ClassName("col-6")
        style = jso {
            justifyContent = justifyContentValue
            display = Display.flex
            alignItems = AlignItems.center
            alignSelf = AlignSelf.start
        }

        if (img != "") {
            @Suppress("MAGIC_NUMBER")
            img {
                src = img
                style = jso {
                    width = "60%".unsafeCast<Width>()
                    border = "0.2rem solid rgb(2, 117, 216)".unsafeCast<Border>()
                }
            }
        }

        if (text != "") {
            marketingTitle("SAVE", false)
        }
    }
}
