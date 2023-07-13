/**
 * Main view for Demos
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import react.FC
import react.Props

import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import web.cssom.*

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
val indexView: FC<IndexViewProps> = FC { props ->
    useBackground(Style.BLUE)
    particles()
    main {
        className = ClassName("main-content")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("grid mt-5")
                logoButtons { }
                props.userInfo ?: run {
                    separator { }
                    indexAuth { props.userInfo }
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

/**
 * properties for index view (user info )
 */
external interface IndexViewProps : Props {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?
}
