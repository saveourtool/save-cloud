/**
 * Main view for Demos
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.ChildrenBuilder

import react.VFC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span
import web.cssom.*

val indexView: VFC = VFC {
    useBackground(Style.BLUE)

    main {
        className = ClassName("main-content")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("grid mt-5")
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
        }
    }

    div {
        className = ClassName("row text-center")

        div {
            className = ClassName("col-3 text-center")
        }
    }
}

private fun ChildrenBuilder.creationCard(url: String, img: String) {
    div {
        className = ClassName("col")
        a {
            href = url
            @Suppress("MAGIC_NUMBER")
            img {
                src = img
                style = jso {
                    width = 20.rem
                    border = "0.2rem solid hsl(186 100% 69%)".unsafeCast<Border>()
                }
            }
        }
    }
}

private fun ChildrenBuilder.neonLightingText(input: String, url: String) {
    a {
        href = url
        div {
            className = ClassName("row")
            div {
                className = ClassName("col text-center")
                button {
                    className = ClassName("glowing-btn")
                    span {
                        className = ClassName("glowing-txt")
                        +input[0]
                        span {
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
