package org.cqfn.save.frontend.components

import kotlinx.html.id
import kotlinx.html.role
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.a
import react.dom.div
import react.dom.i
import react.dom.li
import react.dom.nav
import react.dom.ul

class TopBar : RComponent<RProps, RState>() {
    override fun RBuilder.render() {
        nav("navbar navbar-expand navbar-light bg-white topbar mb-4 static-top shadow") {
            // Topbar Navbar
            ul("navbar-nav ml-auto") {
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
                    }
                    // Dropdown - User Information
                    div("dropdown-menu dropdown-menu-right shadow animated--grow-in") {
                        attrs["aria-labelledby"] = "userDropdown"
                        a("#", classes = "dropdown-item") {
                            i("fas fa-user fa-sm fa-fw mr-2 text-gray-400") {
                                +"Profile"
                            }
                        }
                        a("#", classes = "dropdown-item") {
                            i("fas fa-cogs fa-sm fa-fw mr-2 text-gray-400") {
                                +"Settings"
                            }
                        }
                        a("#", classes = "dropdown-item") {
                            i("fas fa-sign-out fa-sm fa-fw mr-2 text-gray-400") {
                                +"Log out"
                            }
                        }
                    }
                }
            }
        }
    }
}
