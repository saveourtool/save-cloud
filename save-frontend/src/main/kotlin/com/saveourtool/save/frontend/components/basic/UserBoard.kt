/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1
import csstype.ClassName
import react.FC
import react.Props

import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.figure
import react.dom.html.ReactHTML.img

/**
 * React element type that represents user board and can be rendered
 */
val userBoard = userBoard()

/**
 * [Props] for user board component
 */
external interface UserBoardProps : Props {
    /**
     * list of users that should be displayed
     */
    var users: List<UserInfo>
}

/**
 * A functional component to display users' avatars.
 *
 * @return a functional component representing a board of users
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
private fun userBoard() = FC<UserBoardProps> { props ->
    div {
        className = ClassName("latest-photos")
        div {
            className = ClassName("row")
            props.users.forEach { user ->
                div {
                    className = ClassName("col-md-4")
                    figure {
                        img {
                            className = ClassName("img-fluid")
                            src = user.avatar?.let { path ->
                                "/api/$v1/avatar$path"
                            }
                                ?: run {
                                    "img/user.svg"
                                }
                            alt = ""
                        }
                    }
                }
            }
        }
    }
}
