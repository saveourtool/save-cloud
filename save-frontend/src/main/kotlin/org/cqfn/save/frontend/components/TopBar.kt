/**
 * Top bar of web page
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components

import org.cqfn.save.frontend.components.modal.logoutModal
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon

import react.PropsWithChildren
import react.RBuilder
import react.dom.RDOMBuilder
import react.dom.a
import react.dom.attrs
import react.dom.button
import react.dom.div
import react.dom.img
import react.dom.li
import react.dom.nav
import react.dom.ol
import react.dom.span
import react.dom.ul
import react.fc
import react.router.dom.useLocation
import react.useState

import kotlinx.html.BUTTON
import kotlinx.html.ButtonType
import kotlinx.html.classes
import kotlinx.html.id
import kotlinx.html.js.onClickFunction
import kotlinx.html.role

/**
 * [RProps] of the top bor component
 */
external interface TopBarProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userName: String?
}

private fun RBuilder.dropdownEntry(faIcon: String, text: String, handler: RDOMBuilder<BUTTON>.() -> Unit = { }) =
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

    nav("navbar navbar-expand navbar-dark bg-dark topbar mb-3 static-top shadow") {
        // Topbar Navbar
        nav("navbar-nav mr-auto") {
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
                    .split("/")
                    .filterNot { it.isBlank() }
                    .apply {
                        foldIndexed("#") { index: Int, acc: String, pathPart: String ->
                            val currentLink = "$acc/$pathPart"
                            li("breadcrumb-item") {
                                attrs["aria-current"] = "page"
                                if (index == size - 1) {
                                    a(href = currentLink) {
                                        attrs.classes = setOf("text-warning")
                                        +pathPart
                                    }
                                } else {
                                    a(href = currentLink) {
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
                    span("mr-2 d-none d-lg-inline text-gray-600 small") {
                        +(props.userName ?: "Log In")
                    }
                    img(classes = "img-profile rounded-circle", src = "img/undraw_profile.svg") {}
                }
                // Dropdown - User Information
                div("dropdown-menu dropdown-menu-right shadow animated--grow-in") {
                    attrs["aria-labelledby"] = "userDropdown"
                    dropdownEntry("user", "Profile")
                    dropdownEntry("cogs", "Settings")
                    dropdownEntry("sign-out-alt", "Log out") {
                        attrs.onClickFunction = {
                            setIsLogoutModalOpen(true)
                        }
                    }
                }
            }
        }
    }
    logoutModal({
        attrs.isOpen = isLogoutModalOpen
    }) {
        setIsLogoutModalOpen(false)
    }
}
