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

internal val createNewContestTemplate = VFC {
    div {
        className = ClassName("col-lg-2 text-center")
        a {
            className = ClassName("col-12 flex-column card mb-1 box-shadow btn-hov")
            href = "#/${FrontendRoutes.CREATE_CONTESTS_TEMPLATE.path}/"
            style = jso {
                @Suppress("MAGIC_NUMBER")
                height = 19.rem
                textDecoration = "none".unsafeCast<TextDecoration>()
            }
            div {
                className = ClassName("row mb-auto")
                img {
                    src = "img/undraw_create_new.svg"
                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }
                }
            }
            h4 {
                className = ClassName("mb-auto")
                +"Create new contest template"
            }
        }
    }
}
