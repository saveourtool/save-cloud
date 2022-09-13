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

/**
 * The class for analyzing url address and creating links in topBar
 */
class TopBarUrl(href: String) {
    /**
     * currentPath is the link that we put in buttons
     */
    var currentPath = "#"
    private var circumstance: SituationUrlClassification = SituationUrlClassification.KEYWORD_PROCESS
    private var processLastSegments = 0
    private val sizeUrlSegments: Int

    init {
        sizeUrlSegments = href.split("/").size
        findExclude(href)
    }

    /**
     * The function is called to specify the link address in the button before creating the button itself
     *
     * @param pathPart is an appended suffix to an already existing [currentPath]
     */
    fun changeUrlBeforeButton(pathPart: String) {
        currentPath = when (circumstance) {
            SituationUrlClassification.PROJECT_OR_ORGANIZATION -> "#/${FrontendRoutes.PROJECTS.path}"
            SituationUrlClassification.ARCHIVE -> "#/${FrontendRoutes.AWESOME_BENCHMARKS.path}"
            SituationUrlClassification.DETAILS, SituationUrlClassification.EXECUTION -> if (pathPart == "execution") currentPath else mergeUrls(pathPart)
            else -> mergeUrls(pathPart)
        }
    }

    /**
     * The function is called to specify the link address in the button after creating the button itself
     *
     * @param pathPart is an appended suffix to an already existing [currentPath]
     */
    fun changeUrlAfterButton(pathPart: String) {
        fixCurrentPathAfter(pathPart)
        fixExcludeAfter("\\d+".toRegex().matches(pathPart))
    }

    /** The function set a flag whether to create this button or not
     *
     * @param index
     */
    fun isCreateButton(index: Int) = when (circumstance) {
        SituationUrlClassification.KEYWORD_PROCESS_LAST_SEGMENTS -> index >= sizeUrlSegments - 1 - processLastSegments
        SituationUrlClassification.KEYWORD_NOT_PROCESS -> false
        else -> true
    }

    /** The function
     *
     * @param href
     */
    private fun findExclude(href: String) {
        circumstance = if (href.contains(OrganizationMenuBar.regexForUrlClassification) || href.contains(ProjectMenuBar.regexForUrlClassification)) {
            SituationUrlClassification.PROJECT_OR_ORGANIZATION
        } else if (href.contains(BenchmarkCategoryEnum.regexForUrlClassification)) {
            SituationUrlClassification.ARCHIVE
        } else if (href.contains(Regex("/[^/]+/[^/]+/history/execution/[1234567890]+/details"))) {
            SituationUrlClassification.DETAILS
        } else if (href.contains(Regex("/[^/]+/[^/]+/history/execution"))) {
            SituationUrlClassification.EXECUTION
        } else {
            SituationUrlClassification.KEYWORD_PROCESS
        }
    }

    /** Function check exclude and generate currentPath after the buttons creating
     *
     * @param pathPart
     */
    private fun fixCurrentPathAfter(pathPart: String) {
        currentPath = when (circumstance) {
            SituationUrlClassification.PROJECT_OR_ORGANIZATION, SituationUrlClassification.ARCHIVE -> "#"
            SituationUrlClassification.DETAILS, SituationUrlClassification.EXECUTION -> if (pathPart == "execution") mergeUrls(pathPart) else currentPath
            else -> currentPath
        }
    }

    /** The function changes the exclude after the button is created
     *
     * @param isNumber
     */
    private fun fixExcludeAfter(isNumber: Boolean) {
        circumstance = when (circumstance) {
            SituationUrlClassification.PROJECT_OR_ORGANIZATION, SituationUrlClassification.ARCHIVE -> SituationUrlClassification.KEYWORD_PROCESS
            SituationUrlClassification.DETAILS -> if (isNumber) setProcessLastSegments(1) else SituationUrlClassification.DETAILS
            else -> circumstance
        }
    }

    private fun mergeUrls(secondPath: String) = "$currentPath/$secondPath"

    private fun setProcessLastSegments(number: Int): SituationUrlClassification {
        processLastSegments = number
        return SituationUrlClassification.KEYWORD_PROCESS_LAST_SEGMENTS
    }
}

/**
 * ARCHIVE - situation with the processing of the "archive" in the url address - need for tabs in AwesomeBenchmarksView
 * DETAILS - situation with the processing of the "details" in the url address - need for deleted multi-segment urls, starting with the word "details"
 * EXECUTION - situation with the processing of the "execution" in the url address - need for redirect to the page with the executions history
 * KEYWORD_NOT_PROCESS - the button with this url segment is not created
 * KEYWORD_PROCESS - the button with this url segment is created without changes
 * KEYWORD_PROCESS_LAST_SEGMENTS - a button is created if this segment is one of the last
 * PROJECT_OR_ORGANIZATION - situation with the processing of the "archive" in the url address - need for tabs in OrganizationView and ProjectView,
 */
private enum class SituationUrlClassification {
    ARCHIVE,
    DETAILS,
    EXECUTION,
    KEYWORD_NOT_PROCESS,
    KEYWORD_PROCESS,
    KEYWORD_PROCESS_LAST_SEGMENTS,
    PROJECT_OR_ORGANIZATION,
    ;
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
                        val url = TopBarUrl(location.pathname.substringBeforeLast("?"))
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
