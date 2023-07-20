/**
 * Beautiful logos with effects for SAVE and VULN services
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.ChildrenBuilder
import react.VFC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.span
import web.cssom.Border
import web.cssom.ClassName
import web.cssom.rem

val logoButtons = VFC {
    div {
        className = ClassName("row logo-parent mb-5")
        div {
            className = ClassName("col-3 text-center")
        }

        div {
            className = ClassName("col-3 text-center logo-main")
            creationCard(
                "#/${FrontendRoutes.SAVE}",
                "img/save-logo-bg.jpg",
            )
            neonLightingText(
                "SAVE",
                "#/${FrontendRoutes.SAVE}",
                "Cloud Platform for CI and Benchmarking of Code Analyzers"
            )
        }

        div {
            className = ClassName("col-3 text-center logo-main")
            creationCard(
                "#/${FrontendRoutes.VULN}",
                "img/vuln-logo-bg.jpg",
            )

            neonLightingText(
                "VULN",
                "#/${FrontendRoutes.VULN}",
                "Archive of 1-Day Vulnerabilities Aggregated from Various Sources"
            )
        }

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
            (img {
                src = img
                style = jso {
                    width = 17.rem
                    border = "0.2rem solid hsl(186 100% 69%)".unsafeCast<Border>()
                }
            })
        }
    }
}

private fun ChildrenBuilder.neonLightingText(input: String, url: String, labelText: String) {
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

        h3 {
            className = ClassName("mt-5 mx-3 text-white")
            +labelText
        }
    }
}
