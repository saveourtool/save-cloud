/**
 * Top bar of web page
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "WildcardImport")

package org.cqfn.save.frontend.components

import org.cqfn.save.frontend.components.modal.logoutModal
import org.cqfn.save.frontend.externals.fontawesome.faCog
import org.cqfn.save.frontend.externals.fontawesome.faSignOutAlt
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.info.UserInfo

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
            fontAwesomeIcon {
                attrs.icon = faIcon
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
                    attrs.href = "https://github.com/analysis-dev/save"
                    +"SAVE format"
                }
            }
            li("nav-item") {
                a(classes = "nav-link me-2") {
                    attrs["style"] = jso<CSSProperties> {
                        width = 9.rem
                    }.unsafeCast<Width>()
                    attrs.href = "https://github.com/analysis-dev/save-cloud"
                    +"SAVE on GitHub"
                }
            }
            li("nav-item") {
                a(classes = "nav-link me-2") {
                    attrs["style"] = jso<CSSProperties> {
                        width = 8.rem
                    }.unsafeCast<Width>()
                    attrs.href = "#/projects"
                    +"Leaderboard"
                }
            }
            li("nav-item") {
                a(classes = "nav-link me-2") {
                    attrs["style"] = jso<CSSProperties> {
                        width = 6.rem
                    }.unsafeCast<Width>()
                    attrs.href = "https://github.com/analysis-dev/save-cloud"
                    i("fa fa-user opacity-6 text-dark me-1") {
                        attrs["aria-hidden"] = "true"
                    }
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

                    span("mr-2 d-none d-lg-inline text-gray-600") {
                        +(props.userInfo?.name ?: "")
                    }

                    fontAwesomeIcon {
                        attrs.icon = "user"
                        attrs.className = "fas fa-lg fa-fw mr-2 text-gray-400"
                    }
                }
                // Dropdown - User Information
                div("dropdown-menu dropdown-menu-right shadow animated--grow-in") {
                    attrs["aria-labelledby"] = "userDropdown"
                    dropdownEntry(faCog, "Settings") {
                        attrs.onClickFunction = {
                            window.location.href = "#/${props.userInfo?.name}/settings/email"
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
