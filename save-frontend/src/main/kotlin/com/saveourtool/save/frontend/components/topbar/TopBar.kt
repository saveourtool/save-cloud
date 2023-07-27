/**
 * Top bar of web page
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.notIn
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso

import react.*
import react.dom.html.ButtonHTMLAttributes
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.nav
import react.router.useLocation
import web.cssom.ClassName
import web.cssom.vw
import web.html.ButtonType
import web.html.HTMLButtonElement

/**
 * A component for web page top bar.
 *
 * @return a function component
 */
val topBarComponent: FC<TopBarProps> = FC { props ->
    val location = useLocation()
    nav {
        className =
                ClassName("navbar navbar-expand navbar-dark bg-dark topbar mb-3 static-top shadow mr-1 ml-1 rounded")
        style = jso {
            @Suppress("MAGIC_NUMBER")
            width = 100.vw
        }
        id = "navigation-top-bar"
        topBarUrlSplits {
            this.location = location
        }
        if (location.notIn(listOf(FrontendRoutes.REGISTRATION, FrontendRoutes.INDEX, FrontendRoutes.ERROR_404))) {
            topBarLinks { this.location = location }
        }
        topBarUserField {
            userInfo = props.userInfo
        }
    }
}

/**
 * [Props] of the top bar component
 */
external interface TopBarProps : PropsWithChildren {
    /**
     * Currently logged in user, or `null`.
     */
    var userInfo: UserInfo?
}

/**
 * @param faIcon
 * @param text
 * @param isSelected
 * @param handler
 * @return button
 */
fun ChildrenBuilder.dropdownEntry(
    faIcon: FontAwesomeIconModule?,
    text: String,
    isSelected: Boolean = false,
    handler: ChildrenBuilder.(ButtonHTMLAttributes<HTMLButtonElement>) -> Unit = { },
) = button {
    type = ButtonType.button
    val active = if (isSelected) "active" else ""
    className = ClassName("btn btn-no-outline dropdown-item rounded-0 shadow-none $active")
    faIcon?.let {
        fontAwesomeIcon(icon = faIcon) {
            it.className = "fas fa-sm fa-fw mr-2 text-gray-400"
        }
    }
    +text
    handler(this)
}
