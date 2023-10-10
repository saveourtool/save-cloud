/**
 * [1 page] Main information about SAVE-cloud
 */

package com.saveourtool.save.frontend.components.views.welcome.pagers.vuln

import com.saveourtool.save.frontend.components.basic.markdown
import com.saveourtool.save.frontend.components.views.welcome.FIRST_RAW_HEIGHT
import com.saveourtool.save.frontend.externals.i18next.TranslationFunction
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.pre
import react.router.dom.Link
import web.cssom.*

/**
 * rendering 3 paragraphs with info about VULN
 *
 * @param t [TranslationFunction] received from [com.saveourtool.save.frontend.externals.i18next.useTranslation] hook
 */
@Suppress("IDENTIFIER_LENGTH")
fun ChildrenBuilder.renderVulnerabilityGeneralInfo(t: TranslationFunction) {
    div {
        style = jso { color = "rgb(6, 7, 89)".unsafeCast<Color>() }

        div {
            className = ClassName("row justify-content-between mt-5")
            textCard(
                "What is vulnerability?".t(),
                "/img/undraw_question.svg",
                "Vulnerability is a weakness or flaw in a system, network, software, or hardware.".t(),
                "mr-3 px-4",
            )
            textCard(
                "Why is this archive needed?".t(),
                "/img/undraw_share.svg",
                "Archive importance".t(),
                "ml-3 px-4",
            )
        }

        div {
            className = ClassName("row mt-4 align-middle")
            div {
                mdCard("Useful links", "/img/undraw_important.svg", t)
            }
        }
    }
}

private fun ChildrenBuilder.textCard(
    title: String,
    imageUrl: String,
    textStr: String,
    classes: String,
) {
    div {
        className = ClassName("card border border-primary rounded rounded-pill col $classes")
        style = jso {
            height = FIRST_RAW_HEIGHT.rem
        }
        div {
            className = ClassName("d-flex justify-content-center")
            img {
                className = ClassName("rounded m-3")
                src = imageUrl
                style = jso {
                    @Suppress("MAGIC_NUMBER")
                    height = 9.rem
                }
            }
        }
        h5 {
            style = jso {
                textAlign = TextAlign.center
            }
            +title
        }
        p {
            +textStr
        }
    }
}

private fun ChildrenBuilder.mdCard(
    title: String,
    imageUrl: String,
    //markdownStr: String,
    t: TranslationFunction,
) {
    div {
        className = ClassName("card border border-primary rounded rounded-pill col")
        style = jso {
            height = 15.rem
        }
        div {
            className = ClassName("row")
            div {
                className = ClassName("col-3 d-flex align-items-center")
                div {
                    className = ClassName("")
                    img {
                        className = ClassName("rounded pr-4 mx-5 my-3")
                        src = imageUrl
                        style = jso {
                            @Suppress("MAGIC_NUMBER")
                            height = 8.rem
                        }
                    }
                    h5 {
                        style = jso { textAlign = TextAlign.center }
                        +title
                    }
                }
            }
            div {
                className = ClassName("col-8 align-middle pr-4 m-3 overflow-x:hidden")
                ReactHTML.pre {
                    Link {
                        +"OSV Schema "
                        to = "https://ossf.github.io/osv-schema/"
                    }
                    +"OSV Schema".t()
                    Link {
                        +"COSV Schema 1.0 "
                        to = "/${FrontendRoutes.VULN_COSV_SCHEMA}"
                    }
                    +"COSV Schema".t()
                    Link {
                        +"cosv4k "
                        to = "https://github.com/saveourtool/cosv4k"
                    }
                    +"cosv4k".t()
                }
            }
        }
    }
}
