package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import csstype.*
import kotlinx.js.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML
import react.Props


fun ChildrenBuilder.title(title: String) {
    ReactHTML.div {
        className = ClassName("row")
        style = jso {
            justifyContent = JustifyContent.center
            display = Display.flex
        }
        ReactHTML.h4 {
            style = jso {
                color = "#5a5c69".unsafeCast<Color>()
            }
            fontAwesomeIcon(icon = faTrophy)

            className = ClassName("mt-2 mb-4")
            +title
        }
    }
}

 fun ChildrenBuilder.tab(selectedTab: String?, tabsList: List<String>, /*stateSetter: Unit*/) {
    ReactHTML.div {
        className = ClassName("row")
        style = jso {
            justifyContent = JustifyContent.center
            display = Display.flex
        }

        ReactHTML.nav {
            className = ClassName("nav nav-tabs mb-4")
            tabsList.forEachIndexed { i, value ->
                ReactHTML.li {
                    className = ClassName("nav-item")
                    val classVal =
                        if ((i == 0 && selectedTab == null) || selectedTab == value) {
                            " active font-weight-bold"
                        } else {
                            ""
                        }
                    ReactHTML.p {
                        className = ClassName("nav-link $classVal text-gray-800")
                        onClick = {
                            if (selectedTab != value) {
                                // TODO: set state here
                            }
                        }
                        style = jso {
                            cursor = "pointer".unsafeCast<Cursor>()
                        }

                        +value
                    }
                }
            }
        }
    }
}

