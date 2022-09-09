/**
 * Top bar of web page
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components

import com.saveourtool.save.*
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.benchmarks.BenchmarkCategoryEnum
import com.saveourtool.save.frontend.components.modal.logoutModal
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.OrganizationMenuBar
import com.saveourtool.save.frontend.utils.ProjectMenuBar
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.URL_PATH_DELIMITER
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import csstype.rem
import org.w3c.dom.HTMLButtonElement
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
import kotlinx.js.jso

/**
 * [Props] of the top bor component
 */
external interface TopBarProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?
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

class Url(href: String) {
    var currentPath = ""
    private var exception: ExceptionUrlClassification = ExceptionUrlClassification.KEYWORD_PROCESS
    private val sizeUrlSegments: Int

    init {
        currentPath = "#"
        exception = ExceptionUrlClassification.findException(href)
        sizeUrlSegments = href.split("/").size
    }

    fun checkUrlBefore(pathPart: String){
        currentPath = ExceptionUrlClassification.checkCurrentPathBefore(exception, pathPart, currentPath)
    }

    fun checkUrlAfter(pathPart: String) {
        currentPath = ExceptionUrlClassification.checkCurrentPathAfter(exception, pathPart, currentPath)
        exception = ExceptionUrlClassification.checkExceptionAfter(exception, "\\d+".toRegex().matches(pathPart))
    }

    fun isCreateButton(index: Int) = ExceptionUrlClassification.isCreateButton(exception, index, sizeUrlSegments)

    enum class ExceptionUrlClassification {
        ARCHIVE,
        ORGANIZATION,
        PROJECT,
        DETAILS,
        EXECUTION,

        KEYWORD_PROCESS,
        KEYWORD_NOT_PROCESS,
        KEYWORD_PROCESS_ONLY_LAST_SEGMENT,
        ;

        companion object {
            fun findException(href: String) =
                if (href.contains(OrganizationMenuBar.regexForUrlClassification)) {
                    ORGANIZATION
                } else if (href.contains(ProjectMenuBar.regexForUrlClassification)) {
                    PROJECT
                } else if (href.contains(BenchmarkCategoryEnum.regexForUrlClassification)) {
                    ARCHIVE
                } else if (href.contains(Regex("/[^/]+/[^/]+/history/execution/[1234567890]+/details"))) {
                    DETAILS
                } else if (href.contains("/[^/]+/[^/]+/history/execution")) {
                    EXECUTION
                } else {
                    KEYWORD_PROCESS
                }

            fun checkCurrentPathBefore(
                exception: ExceptionUrlClassification,
                pathPart: String,
                allPath: String,
            ) = when (exception) {
                ORGANIZATION -> "#/${FrontendRoutes.CONTESTS_GLOBAL_RATING.path}"
                PROJECT -> "#/${FrontendRoutes.PROJECTS.path}"
                ARCHIVE -> "#/${FrontendRoutes.AWESOME_BENCHMARKS.path}"
                DETAILS, EXECUTION -> if (pathPart == "execution") allPath else mergeUrls(allPath, pathPart)
                else -> mergeUrls(allPath, pathPart)
            }

            fun checkCurrentPathAfter(
                exception: ExceptionUrlClassification,
                pathPart: String,
                allPath: String
            ) = when(exception) {
                ORGANIZATION, PROJECT, ARCHIVE -> "#"
                DETAILS, EXECUTION -> if (pathPart == "execution") mergeUrls(allPath, pathPart) else allPath
                else -> allPath
            }

            fun checkExceptionAfter(
                exception: ExceptionUrlClassification,
                isNumber: Boolean
            ) = when (exception) {
                ORGANIZATION, PROJECT, ARCHIVE -> KEYWORD_PROCESS
                DETAILS -> if (isNumber) KEYWORD_PROCESS_ONLY_LAST_SEGMENT else DETAILS
                else -> exception
            }

            fun isCreateButton(exception: ExceptionUrlClassification, index: Int, size: Int) = when (exception) {
                KEYWORD_PROCESS_ONLY_LAST_SEGMENT -> index == size - 2
                KEYWORD_NOT_PROCESS -> false
                else -> true
            }
            private fun mergeUrls(firstPath: String, secondPath: String) = "$firstPath/$secondPath"
        }
    }
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
    val (isLogoutModalOpen, setIsLogoutModalOpen) = useState(false)
    val location = useLocation()
    val scope = CoroutineScope(Dispatchers.Default)
    useEffect {
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }

    nav {
        className = ClassName("navbar navbar-expand navbar-dark bg-dark topbar mb-3 static-top shadow mr-1 ml-1 rounded")
        id = "navigation-top-bar"

        // Top bar Navbar
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
                        val textColor = if (location.pathname == "/") "text-warning" else "text-light"
                        className = ClassName(textColor)
                        +"SAVE"
                    }
                }
                location.pathname
                    .substringBeforeLast("?")
                    .split(URL_PATH_DELIMITER)
                    .filterNot { it.isBlank() }
                    .apply {
                        val url = Url(location.pathname.substringBeforeLast("?"))
                        forEachIndexed { index: Int, pathPart: String ->
                            url.checkUrlBefore(pathPart)
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
                            url.checkUrlAfter(pathPart)
                        }
                    }
            }
        }

        ul {
            className = ClassName("navbar-nav mx-auto")
            li {
                className = ClassName("nav-item")
                a {
                    val hrefAnchor = FrontendRoutes.AWESOME_BENCHMARKS.path
                    className = ClassName("nav-link d-flex align-items-center me-2 ${textColor(hrefAnchor, location)} active")
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
                    className = ClassName("nav-link d-flex align-items-center me-2 active")
                    style = jso {
                        width = 8.rem
                    }
                    href = "https://github.com/saveourtool/save-cli"
                    +"SAVE format"
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
                    val hrefAnchor = FrontendRoutes.PROJECTS.path
                    className = ClassName("nav-link d-flex align-items-center me-2 ${textColor(hrefAnchor, location)} active ")
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
                    val hrefAnchor = FrontendRoutes.CONTESTS.path
                    className = ClassName("nav-link d-flex align-items-center me-2 ${textColor(hrefAnchor, location)} active")
                    style = jso {
                        width = 6.rem
                    }
                    href = "#/$hrefAnchor"
                    +"Contests"
                }
            }
            li {
                className = ClassName("nav-item")
                a {
                    val hrefAnchor = "about"
                    className = ClassName("nav-link d-flex align-items-center me-2 ${textColor(hrefAnchor, location)} active")
                    style = jso {
                        width = 6.rem
                    }
                    href = "#/$hrefAnchor"
                    +"About"
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
                a {
                    href = "#"
                    className = ClassName("nav-link dropdown-toggle")
                    id = "userDropdown"
                    role = "button".unsafeCast<AriaRole>()
                    ariaExpanded = false
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
                    className = ClassName("dropdown-menu dropdown-menu-right shadow animated--grow-in")
                    ariaLabelledBy = "userDropdown"
                    props.userInfo?.name?.let { name ->
                        dropdownEntry(faCog, "Settings") { attrs ->
                            attrs.onClick = {
                                window.location.href = "#/$name/${FrontendRoutes.SETTINGS_EMAIL.path}"
                            }
                        }
                        dropdownEntry(faCity, "My organizations") { attrs ->
                            attrs.onClick = {
                                window.location.href = "#/$name/${FrontendRoutes.SETTINGS_ORGANIZATIONS.path}"
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
    }

    logoutModal {
        setIsLogoutModalOpen(false)
    }() {
        isOpen = isLogoutModalOpen
    }
}

private fun textColor(hrefAnchor: String, location: history.Location) =
        if (location.pathname.endsWith(hrefAnchor)) "text-warning" else "text-light"
