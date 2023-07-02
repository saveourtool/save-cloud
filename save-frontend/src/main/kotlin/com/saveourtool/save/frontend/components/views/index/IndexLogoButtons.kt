package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.ChildrenBuilder
import react.VFC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import web.cssom.Border
import web.cssom.ClassName
import web.cssom.rem

val logoButtons = VFC {
    div {
        className = ClassName("row logo-parent")
        div {
            className = ClassName("col-3 text-center")
        }

        div {
            className = ClassName("col-3 text-center logo-main")
            creationCard(
                "#/${FrontendRoutes.SAVE}",
                "img/save-logo-bg.jpg",
            )
            neonLightingText("SAVE", "#/${FrontendRoutes.SAVE}")
        }

        div {
            className = ClassName("col-3 text-center logo-main")
            creationCard(
                "#/${FrontendRoutes.VULNERABILITIES}",
                "img/vuln-logo-bg.jpg",
            )

            neonLightingText("VULN", "#/${FrontendRoutes.VULNERABILITIES}")
        }

        div {
            className = ClassName("col-3 text-center")
        }
    }
}

private fun ChildrenBuilder.creationCard(url: String, img: String) {
    ReactHTML.div {
        className = ClassName("col")
        ReactHTML.a {
            href = url
            @Suppress("MAGIC_NUMBER")
            (ReactHTML.img {
                src = img
                style = jso {
                    width = 20.rem
                    border = "0.2rem solid hsl(186 100% 69%)".unsafeCast<Border>()
                }
            })
        }
    }
}

private fun ChildrenBuilder.neonLightingText(input: String, url: String) {
    ReactHTML.a {
        href = url
        ReactHTML.div {
            className = ClassName("row")
            ReactHTML.div {
                className = ClassName("col text-center")
                ReactHTML.button {
                    className = ClassName("glowing-btn")
                    ReactHTML.span {
                        className = ClassName("glowing-txt")
                        +input[0]
                        ReactHTML.span {
                            className = ClassName("faulty-letter")
                            +input[1]
                        }
                        +input.substring(2)
                    }
                }
            }
        }
    }
}
