/**
 * Top bar of web page
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.components.modal.logoutModal
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.info.UserInfo

import csstype.Width
import csstype.rem
import react.*
import react.dom.*
import react.fc
import react.router.useLocation
import react.useState

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.html.BUTTON
import kotlinx.html.ButtonType
import kotlinx.html.classes
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import kotlinx.html.role
import kotlinx.js.jso

/**
 * [RProps] of the top bor component
 */
external interface TopBarProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?
}

private fun RBuilder.dropdownEntry(faIcon: dynamic, text: String, handler: RDOMBuilder<BUTTON>.() -> Unit = { }) =
        button(type = ButtonType.button, classes = "btn btn-no-outline dropdown-item rounded-0 shadow-none") {
            fontAwesomeIcon(icon = faIcon) {
                attrs.className = "fas fa-sm fa-fw mr-2 text-gray-400"
            }
            +text
            handler(this)
        }

/**
 * A component for web page top bar
 *
 * @return a function component
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun topBar() = fc<TopBarProps> { props ->
    val (isLogoutModalOpen, setIsLogoutModalOpen) = useState(false)
    val location = useLocation()
    val scope = CoroutineScope(Dispatchers.Default)
    useEffect(listOf<dynamic>()) {
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }

    nav("navbar navbar-expand navbar-dark bg-dark topbar mb-3 static-top shadow mr-1 ml-1 rounded") {
        attrs.id = "navigation-top-bar"

        // Topbar Navbar
        nav("navbar-nav mr-auto w-100") {
            attrs["aria-label"] = "breadcrumb"
            ol("breadcrumb mb-0") {
                li("breadcrumb-item") {
                    attrs["aria-current"] = "page"
                    a(href = "#/") {
                        attrs.classes = setOf("text-light")
                        +"SAVE"
                    }
                }
                location.pathname
                    .substringBeforeLast("?")
                    .split("/")
                    .filterNot { it.isBlank() }
                    .apply {
                        foldIndexed("#") { index: Int, acc: String, pathPart: String ->

                            val currentLink = "$acc/$pathPart"

                            li("breadcrumb-item") {
                                attrs["aria-current"] = "page"
                                if (index == size - 1) {
                                    a {
                                        attrs.classes = setOf("text-warning")
                                        +pathPart
                                    }
                                } else {
                                    // small hack to redirect from history/execution to history
                                    val resultingLink = currentLink.removeSuffix("/execution")
                                    a(resultingLink) {
                                        attrs.classes = setOf("text-light")
                                        +pathPart
                                    }
                                }
                            }
                            currentLink
                        }
                    }
            }
        }

        ul("navbar-nav mx-auto") {
            li("nav-item") {
                a(classes = "nav-link d-flex align-items-center me-2 active") {
                    attrs["style"] = jso<CSSProperties> {
                        width = 12.rem
                    }.unsafeCast<Width>()
                    attrs.href = "#/awesome-benchmarks"
                    +"Awesome Benchmarks"
                }
            }
            li("nav-item") {
                a(classes = "nav-link d-flex align-items-center me-2 active") {
                    attrs["style"] = jso<CSSProperties> {
                        width = 8.rem
                    }.unsafeCast<Width>()
                    attrs.href = "https://github.com/saveourtool/save-cli"
                    +"SAVE format"
                }
            }
            li("nav-item") {
                a(classes = "nav-link me-2") {
                    attrs["style"] = jso<CSSProperties> {
                        width = 9.rem
                    }.unsafeCast<Width>()
                    attrs.href = "https://github.com/saveourtool/save-cloud"
                    +"SAVE on GitHub"
                }
            }
            li("nav-item") {
                a(classes = "nav-link me-2") {
                    attrs["style"] = jso<CSSProperties> {
                        width = 8.rem
                    }.unsafeCast<Width>()
                    attrs.href = "#/projects"
                    +"Projects board"
                }
            }
            li("nav-item") {
                a(classes = "nav-link me-2") {
                    attrs["style"] = jso<CSSProperties> {
                        width = 6.rem
                    }.unsafeCast<Width>()
                    attrs.href = "https://github.com/saveourtool/save-cloud"
                    fontAwesomeIcon(icon = faUser, classes = "fa opacity-6 text-dark me-1")
                    +"About"
                }
            }
        }

        ul("navbar-nav ml-auto") {
            div("topbar-divider d-none d-sm-block") {}
            // Nav Item - User Information
            li("nav-item dropdown no-arrow") {
                a("#", classes = "nav-link dropdown-toggle") {
                    attrs {
                        id = "userDropdown"
                        role = "button"
                        set("data-toggle", "dropdown")
                        set("aria-haspopup", "true")
                        set("aria-expanded", "false")
                    }

                    div("row") {
                        div {
                            span("mr-2 d-none d-lg-inline text-gray-600") {
                                +(props.userInfo?.name ?: "")
                            }
                            fontAwesomeIcon(icon = faUser) {
                                attrs.className = "fas fa-lg fa-fw mr-2 text-gray-400"
                            }
                        }
                        val globalRole = props.userInfo?.globalRole ?: Role.VIEWER
                        if (globalRole.priority >= Role.ADMIN.priority) {
                            small("text-gray-400 text-justify") {
                                +globalRole.formattedName
                            }
                        }
                    }
                }
                // Dropdown - User Information
                div("dropdown-menu dropdown-menu-right shadow animated--grow-in") {
                    attrs["aria-labelledby"] = "userDropdown"
                    props.userInfo?.name?.let { name ->
                        dropdownEntry(faCog, "Settings") {
                            attrs.onClickFunction = {
                                window.location.href = "#/$name/settings/email"
                            }
                        }
                        dropdownEntry(faCity, "My organizations") {
                            attrs.onClickFunction = {
                                window.location.href = "#/$name/settings/organizations"
                            }
                        }
                    }
                    dropdownEntry(faSignOutAlt, "Log out") {
                        attrs.onClickFunction = {
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
        attrs.isOpen = isLogoutModalOpen
    }
}
