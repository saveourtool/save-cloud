package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.frontend.externals.fontawesome.FontAwesomeIconModule
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import csstype.*
import kotlinx.js.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML


fun ChildrenBuilder.title(title: String, icon: FontAwesomeIconModule) {
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
            fontAwesomeIcon(icon = icon)

            className = ClassName("mt-2 mb-4")
            +title
        }
    }
}

 fun ChildrenBuilder.tab(selectedTab: String?, tabsList: List<String>, updateTabState: (String) -> Unit) {
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
                            kotlinx.js.console.log(value)
                            kotlinx.js.console.log(selectedTab)
                            if (selectedTab != value) {
                                updateTabState(value)
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

