/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.figure
import web.cssom.ClassName

/**
 * A functional component to display users' avatars.
 */
val userBoard: FC<UserBoardProps> = FC { props ->
    div {
        className = ClassName("latest-photos")
        div {
            className = ClassName("row")
            props.users.forEach { user ->
                div {
                    className = ClassName(props.avatarOuterClasses.orEmpty())
                    figure {
                        renderAvatar(user, props.avatarInnerClasses.orEmpty(), "/${FrontendRoutes.PROFILE}/${user.name}")
                    }
                }
            }
        }
    }
}

/**
 * [Props] for [userBoard] component
 */
external interface UserBoardProps : Props {
    /**
     * list of users that should be displayed
     */
    var users: List<UserInfo>

    /**
     * Classes that are applied to [div] that contains img tag
     */
    var avatarOuterClasses: String?

    /**
     * Classes that are applied to img tag
     */
    var avatarInnerClasses: String?
}
