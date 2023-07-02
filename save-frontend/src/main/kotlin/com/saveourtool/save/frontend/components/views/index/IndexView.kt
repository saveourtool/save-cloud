/**
 * Main view for Demos
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.utils.*

import react.VFC
import react.dom.html.ReactHTML.div
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
                logoButtons { }
                indexAuth { }
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


