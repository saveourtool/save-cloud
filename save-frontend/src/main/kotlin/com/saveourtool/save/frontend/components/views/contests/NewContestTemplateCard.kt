@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.VFC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import web.cssom.*

val newContestTemplate = newContestTemplateCard()

private fun newContestTemplateCard() = VFC {
    div {
        className = ClassName("col-lg-2")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            @Suppress("MAGIC_NUMBER")
            style = jso {
                height = 14.rem
            }

            div {
                className = ClassName("col-lg-6 link-container")
                @Suppress("MAGIC_NUMBER")
                style = jso {
                    minHeight = 12.rem
                }

                a {
                    className = ClassName("link-three ml-5")
                    href = "#/${FrontendRoutes.CREATE_CONTESTS_TEMPLATE.path}/"
                    style = jso {
                        textDecoration = "none".unsafeCast<TextDecoration>()
                    }
                    h4 {
                        +"Create"
                    }
                    h4 {
                        +"new"
                    }
                    h4 {
                        +"contest"
                    }
                    h4 {
                        +"template"
                    }
                }
            }

            div {
                className = ClassName("col-lg-6 justify-content-center align-items-center")
                img {
                    src = "img/undraw_for_review_eqxk.svg"
                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }
                }
            }
        }
    }
}
