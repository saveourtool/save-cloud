/**
 * Main view for Demos
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import react.FC
import react.Props

import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import web.cssom.*

val indexView: FC<IndexViewProps> = FC {props ->
    useBackground(Style.BLUE)
    main {
        className = ClassName("main-content")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("grid mt-5")
                logoButtons { }
                indexAuth { props.userInfo }
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

external interface IndexViewProps : Props {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?
}
