/**
 * Top bar of web page
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components

import com.saveourtool.save.*
import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.components.modal.logoutModal
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.TopBarUrl
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.SAVE_CLOUD_GITHUB
import com.saveourtool.save.utils.URL_PATH_DELIMITER
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import csstype.Width
import csstype.rem
import dom.html.HTMLButtonElement
import history.Location
import react.router.dom.Link
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
import react.router.useNavigate
import react.useState

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive


/**
 * Displays the url and its division by "/"
 */
private val topBarUrlSplits: FC<TopBarPropsWithLocation> = FC { props ->
    nav {
        className = ClassName("navbar-nav mr-auto w-100")
        ariaLabel = "breadcrumb"
        ol {
            className = ClassName("breadcrumb mb-0")
            li {
                className = ClassName("breadcrumb-item")
                ariaCurrent = "page".unsafeCast<AriaCurrent>()
                Link {
                    to = "#/"
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
                                    Link {
                                        className = ClassName("text-warning")
                                        +pathPart
                                    }
                                } else {
                                    Link {
                                        to = url.currentPath
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

/**
 * Displays the static links that do not depend on the url
 */
@Suppress("MAGIC_NUMBER")
private val topBarLinks: FC<TopBarPropsWithLocation> = FC { props ->
    val (isDemoDropdownActive, setIsDemoDropdownActive) = useState(false)
    val topBarlinksList = listOf(
        TopBarLink(hrefAnchor = FrontendRoutes.AWESOME_BENCHMARKS.path, width = 12.rem, text = "Awesome Benchmarks"),
        TopBarLink(hrefAnchor = FrontendRoutes.SANDBOX.path, width = 9.rem, text = "Try SAVE format"),
        TopBarLink(hrefAnchor = SAVE_CLOUD_GITHUB, width = 9.rem, text = "SAVE on GitHub"),
        TopBarLink(hrefAnchor = FrontendRoutes.PROJECTS.path, width = 8.rem, text = "Projects board"),
        TopBarLink(hrefAnchor = FrontendRoutes.CONTESTS.path, width = 6.rem, text = "Contests"),
        TopBarLink(hrefAnchor = FrontendRoutes.ABOUT_US.path, width = 6.rem, text = "About us")
    )

    ul {
        className = ClassName("navbar-nav mx-auto")
        li {
            className = ClassName("nav-item dropdown no-arrow")
            style = jso {
                width = 5.rem
            }
            a {
                className = ClassName("nav-link dropdown-toggle text-light")
                asDynamic()["data-toggle"] = "dropdown"
                ariaExpanded = false
                id = "demoDropdown"
                role = "button".unsafeCast<AriaRole>()
                +"Demo"
                onClickCapture = { _ ->
                    setIsDemoDropdownActive { !it }
                }
            }
            div {
                className = ClassName("dropdown-menu dropdown-menu-right shadow animated--grow-in${if (isDemoDropdownActive) " show" else "" }")
                ariaLabelledBy = "demoDropdown"
                val diktatDemoHref = "#/${FrontendRoutes.DEMO.path}/diktat"
                dropdownEntry(null, "Diktat", window.location.href.contains(diktatDemoHref)) { attrs ->
                    attrs.onClick = {
                        setIsDemoDropdownActive(false)
                        window.location.href = diktatDemoHref
                    }
                }
                val cpgDemoHref = "#/${FrontendRoutes.DEMO.path}/cpg"
                dropdownEntry(null, "CPG", window.location.href.contains(cpgDemoHref)) { attrs ->
                    attrs.onClick = {
                        setIsDemoDropdownActive(false)
                        window.location.href = cpgDemoHref
                    }
                }
            }
        }
        topBarlinksList.forEach { elem ->
            val isNotSaveCloudLink = elem.hrefAnchor != SAVE_CLOUD_GITHUB
            val elemClassName = if (isNotSaveCloudLink) textColor(elem.hrefAnchor, props.location) else ""
            li {
                className = ClassName("nav-item")
                Link {
                    className = ClassName("nav-link d-flex align-items-center me-2 $elemClassName active")
                    style = jso { width = elem.width }
                    to = elem.hrefAnchor
                    // href = if (isNotSaveCloudLink) "#/${elem.hrefAnchor}" else linkToSaveCloudOnGithub
                    +elem.text
                }
            }
        }
    }
}

/**
 * Displays the user's field
 */
@Suppress("MAGIC_NUMBER")
private val topBarUserField: FC<TopBarProps> = FC { props ->
    val (isLogoutModalOpen, setIsLogoutModalOpen) = useState(false)
    val (isAriaExpanded, setIsAriaExpanded) = useState(false)
    val scope = CoroutineScope(Dispatchers.Default)
    val navigate = useNavigate()
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
            a {
                href = "#"
                className = ClassName("nav-link dropdown-toggle")
                id = "userDropdown"
                role = "button".unsafeCast<AriaRole>()
                ariaExpanded = isAriaExpanded
                ariaHasPopup = true.unsafeCast<AriaHasPopup>()
                asDynamic()["data-toggle"] = "dropdown"

                div {
                    className = ClassName("d-flex flex-row")
                    div {
                        className = ClassName("d-flex flex-column")
                        span {
                            className = ClassName("mr-2 d-none d-lg-inline text-gray-600")
                            +(props.userInfo?.name ?: "")
                        }
                        val globalRole = props.userInfo?.globalRole ?: Role.VIEWER
                        if (globalRole.isHigherOrEqualThan(Role.ADMIN)) {
                            small {
                                className = ClassName("text-gray-400 text-justify")
                                +globalRole.formattedName
                            }
                        }
                    }
                    props.userInfo?.avatar?.let {
                        img {
                            className =
                                    ClassName("ml-2 align-self-center avatar avatar-user width-full border color-bg-default rounded-circle fas mr-2")
                            src = "/api/$v1/avatar$it"
                            height = 45.0
                            width = 45.0
                        }
                    } ?: fontAwesomeIcon(icon = faUser) {
                        it.className = "m-2 align-self-center fas fa-lg fa-fw mr-2 text-gray-400"
                    }
                }
            }
            // Dropdown - User Information
            div {
                className = ClassName("dropdown-menu dropdown-menu-right shadow animated--grow-in${if (isAriaExpanded) " show" else ""}")
                ariaLabelledBy = "userDropdown"
                props.userInfo?.name?.let { name ->
                    dropdownEntry(faCog, "Settings") { attrs ->
                        attrs.onClick = {
                            navigate(to = "/$name/${FrontendRoutes.SETTINGS_EMAIL.path}")
                        }
                    }
                    dropdownEntry(
                        faCity,
                        "My organizations"
                    ) { attrs ->
                        attrs.onClick = {
                            navigate(to = "/$name/${FrontendRoutes.SETTINGS_ORGANIZATIONS.path}")
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

/**
 * [Props] of the top bor component with [location]
 */
external interface TopBarPropsWithLocation : TopBarProps {
    /**
     * Is user location
     */
    var location: Location
}

/**
 * @property hrefAnchor is link
 * @property width is width of the link text
 * @property text is link text
 */
data class TopBarLink(
    val hrefAnchor: String,
    val width: Width,
    val text: String,
)

private fun ChildrenBuilder.dropdownEntry(
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

/**
 * A component for web page top bar
 *
 * @return a function component
 */
fun topBar() = FC<TopBarProps> { props ->
    val location = useLocation()
    nav {
        className = ClassName("navbar navbar-expand navbar-dark bg-dark topbar mb-3 static-top shadow mr-1 ml-1 rounded")
        id = "navigation-top-bar"
        topBarUrlSplits {
            userInfo = props.userInfo
            isMobileScreen = props.isMobileScreen
            this.location = location
        }
        topBarLinks {
            userInfo = props.userInfo
            isMobileScreen = props.isMobileScreen
            this.location = location
        }
        topBarUserField {
            userInfo = props.userInfo
            isMobileScreen = props.isMobileScreen
        }
    }
}

private fun textColor(hrefAnchor: String, location: Location) =
        if (location.pathname.endsWith(hrefAnchor)) "text-warning" else "text-light"
