/**
 * Top bar of web page
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components

import com.saveourtool.save.*
import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.components.modal.logoutModal
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.ButtonWithActionProps
import com.saveourtool.save.frontend.utils.TopBarUrl
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.URL_PATH_DELIMITER
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import csstype.rem
import dom.html.HTMLButtonElement
import history.Location
import js.core.jso
import react.*
import react.dom.aria.*
import react.dom.html.ButtonHTMLAttributes
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.ol
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul
import react.router.useLocation
import react.useState

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.datetime.internal.JSJoda.use

/**
 * [Props] of the top bor component
 */
external interface TopBarProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?

    /**
     * true if the device is mobile (screen is less 1000px)
     */
    var isMobileScreen: Boolean?
}

private fun ChildrenBuilder.dropdownEntry(
    faIcon: dynamic,
    text: String,
    handler: ChildrenBuilder.(ButtonHTMLAttributes<HTMLButtonElement>) -> Unit = { },
) = button {
    type = ButtonType.button
    className = ClassName("btn btn-no-outline dropdown-item rounded-0 shadow-none")
    fontAwesomeIcon(icon = faIcon) {
        it.className = "fas fa-sm fa-fw mr-2 text-gray-400"
    }
    +text
    handler(this)
}

/**
 * A component for web page top bar
 *
 * @return a function component
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "ComplexMethod",
    "TOO_MANY_LINES_IN_LAMBDA"
)
fun topBar() = FC<TopBarProps> { props ->
    val locationU = useLocation()
    topBarUrlSplits{
        userInfo = props.userInfo
        isMobileScreen = props.isMobileScreen
        location = locationU
    }
    topBarLinks{
        userInfo = props.userInfo
        isMobileScreen = props.isMobileScreen
        location = locationU
    }
    topBarUserField{
        userInfo = props.userInfo
        isMobileScreen = props.isMobileScreen
    }
}


/**
 * [Props] of the top bor component
 */
external interface TopBarPropsWithLocation : TopBarProps {
    /**
     * Currently logged in user or null
     */
    var location: Location
}

private fun textColor(hrefAnchor: String, location: Location) =
        if (location.pathname.endsWith(hrefAnchor)) "text-warning" else "text-light"

private val topBarUrlSplits: FC<TopBarPropsWithLocation> = FC { props ->
    nav {
        className = ClassName("navbar-nav mr-auto w-100")
        ariaLabel = "breadcrumb"
        ol {
            className = ClassName("breadcrumb mb-0")
            li {
                className = ClassName("breadcrumb-item")
                ariaCurrent = "page".unsafeCast<AriaCurrent>()
                a {
                    href = "#/"
                    // if we are on welcome page right now - need to highlight SAVE in menu
                    val textColor = if (props.location.pathname == "/") "text-warning" else "text-light"
                    className = ClassName(textColor)
                    +"SAVE"
                }
            }
            props.location.pathname
                .substringBeforeLast("?")
                .split(URL_PATH_DELIMITER)
                .filterNot { it.isBlank() }
                .apply {
                    val url = TopBarUrl(props.location.pathname.substringBeforeLast("?"))
                    forEachIndexed { index: Int, pathPart: String ->
                        url.changeUrlBeforeButton(pathPart)
                        if (url.isCreateButton(index)) {
                            li {
                                className = ClassName("breadcrumb-item")
                                ariaCurrent = "page".unsafeCast<AriaCurrent>()
                                if (index == size - 1) {
                                    a {
                                        className = ClassName("text-warning")
                                        +pathPart
                                    }
                                } else {
                                    a {
                                        href = url.currentPath
                                        className = ClassName("text-light")
                                        +pathPart
                                    }
                                }
                            }
                        }
                        url.changeUrlAfterButton(pathPart)
                    }
                }
        }
    }
}

private val topBarLinks: FC<TopBarPropsWithLocation> = FC { props ->
    ul {
        className = ClassName("navbar-nav mx-auto")
        li {
            className = ClassName("nav-item")
            a {
                val hrefAnchor = com.saveourtool.save.validation.FrontendRoutes.AWESOME_BENCHMARKS.path
                className = ClassName(
                    "nav-link d-flex align-items-center me-2 ${
                        textColor(
                            hrefAnchor,
                            props.location
                        )
                    } active"
                )
                style = jso {
                    width = 12.rem
                }
                href = "#/$hrefAnchor"
                +"Awesome Benchmarks"
            }
        }
        li {
            className = ClassName("nav-item")
            a {
                val hrefAnchor = "${com.saveourtool.save.validation.FrontendRoutes.DEMO.path}/cpg"
                className = ClassName(
                    "nav-link d-flex align-items-center me-2 ${
                        textColor(
                            hrefAnchor,
                            props.location
                        )
                    } active"
                )
                style = jso {
                    width = 3.5.rem
                }
                href = "#/$hrefAnchor"
                +"CPG"
            }
        }
        li {
            className = ClassName("nav-item")
            a {
                val hrefAnchor = com.saveourtool.save.validation.FrontendRoutes.SANDBOX.path
                className = ClassName(
                    "nav-link d-flex align-items-center me-2 ${
                        textColor(
                            hrefAnchor,
                            props.location
                        )
                    } active"
                )
                style = jso {
                    width = 9.rem
                }
                href = "#/$hrefAnchor"
                +"Try SAVE format"
            }
        }
        li {
            className = ClassName("nav-item")
            a {
                className = ClassName("nav-link d-flex align-items-center me-2 active")
                style = jso {
                    width = 9.rem
                }
                href = "https://github.com/saveourtool/save-cloud"
                +"SAVE on GitHub"
            }
        }
        li {
            className = ClassName("nav-item")
            a {
                val hrefAnchor = com.saveourtool.save.validation.FrontendRoutes.PROJECTS.path
                className = ClassName(
                    "nav-link d-flex align-items-center me-2 ${
                        textColor(
                            hrefAnchor,
                            props.location
                        )
                    } active "
                )
                style = jso {
                    width = 8.rem
                }
                href = "#/$hrefAnchor"
                +"Projects board"
            }
        }
        li {
            className = ClassName("nav-item")
            a {
                val hrefAnchor = com.saveourtool.save.validation.FrontendRoutes.CONTESTS.path
                className = ClassName(
                    "nav-link d-flex align-items-center me-2 ${
                        textColor(
                            hrefAnchor,
                            props.location
                        )
                    } active"
                )
                style = jso {
                    width = 6.rem
                }
                href = "#/$hrefAnchor"
                +"Contests"
            }
        }
        li {
            className = ClassName("nav-item")
            react.dom.html.ReactHTML.a {
                val hrefAnchor = com.saveourtool.save.validation.FrontendRoutes.ABOUT_US.path
                className = ClassName(
                    "nav-link d-flex align-items-center me-2 ${
                        textColor(
                            hrefAnchor,
                            props.location
                        )
                    } active"
                )
                style = jso {
                    width = 6.rem
                }
                href = "#/$hrefAnchor"
                +"About us"
            }
        }
    }
}

private val topBarUserField: FC<TopBarProps> = FC { props ->
    val (isLogoutModalOpen, setIsLogoutModalOpen) = useState(false)
    val (isAriaExpanded, setIsAriaExpanded) = useState(false)
    val scope = CoroutineScope(Dispatchers.Default)
    useEffect {
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }
    ul {
        className = ClassName("navbar-nav ml-auto")
        div {
            className = ClassName("topbar-divider d-none d-sm-block")
        }
        // Nav Item - User Information
        li {
            className = ClassName("nav-item dropdown no-arrow")
            onClickCapture = {
                setIsAriaExpanded {
                    !it
                }
            }
            react.dom.html.ReactHTML.a {
                href = "#"
                className = ClassName("nav-link dropdown-toggle")
                id = "userDropdown"
                role = "button".unsafeCast<AriaRole>()
                ariaExpanded = isAriaExpanded
                ariaHasPopup = true.unsafeCast<AriaHasPopup>()
                asDynamic()["data-toggle"] = "dropdown"

                react.dom.html.ReactHTML.div {
                    className = ClassName("d-flex flex-row")
                    react.dom.html.ReactHTML.div {
                        className = ClassName("d-flex flex-column")
                        react.dom.html.ReactHTML.span {
                            className = ClassName("mr-2 d-none d-lg-inline text-gray-600")
                            +(props.userInfo?.name ?: "")
                        }
                        val globalRole = props.userInfo?.globalRole ?: com.saveourtool.save.domain.Role.VIEWER
                        if (globalRole.isHigherOrEqualThan(com.saveourtool.save.domain.Role.ADMIN)) {
                            react.dom.html.ReactHTML.small {
                                className = ClassName("text-gray-400 text-justify")
                                +globalRole.formattedName
                            }
                        }
                    }
                    props.userInfo?.avatar?.let {
                        react.dom.html.ReactHTML.img {
                            className =
                                ClassName("ml-2 align-self-center avatar avatar-user width-full border color-bg-default rounded-circle fas mr-2")
                            src = "/api/${v1}/avatar$it"
                            height = 45.0
                            width = 45.0
                        }
                    } ?: fontAwesomeIcon(icon = faUser) {
                        it.className = "m-2 align-self-center fas fa-lg fa-fw mr-2 text-gray-400"
                    }
                }
            }
            // Dropdown - User Information
            react.dom.html.ReactHTML.div {
                className = ClassName("dropdown-menu dropdown-menu-right shadow animated--grow-in${if (isAriaExpanded) " show" else ""}")
                ariaLabelledBy = "userDropdown"
                props.userInfo?.name?.let { name ->
                    dropdownEntry(faCog, "Settings") { attrs ->
                        attrs.onClick = {
                            window.location.href = "#/$name/${com.saveourtool.save.validation.FrontendRoutes.SETTINGS_EMAIL.path}"
                        }
                    }
                    dropdownEntry(
                        faCity,
                        "My organizations"
                    ) { attrs ->
                        attrs.onClick = {
                            window.location.href =
                                "#/$name/${com.saveourtool.save.validation.FrontendRoutes.SETTINGS_ORGANIZATIONS.path}"
                        }
                    }
                }
                dropdownEntry(faSignOutAlt, "Log out") { attrs ->
                    attrs.onClick = {
                        setIsLogoutModalOpen(true)
                    }
                }
            }
        }
    }

    logoutModal {
        setIsLogoutModalOpen(false)
    }() {
        isOpen = isLogoutModalOpen
    }
}