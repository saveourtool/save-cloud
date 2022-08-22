/**
 * Utility methods used for rendering of contest list view
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.frontend.externals.fontawesome.FontAwesomeIconModule
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import csstype.*
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p

import kotlinx.js.jso

/**
 * @param title
 * @param icon
 */
fun ChildrenBuilder.title(title: String, icon: FontAwesomeIconModule) {
    div {
        className = ClassName("row")
        style = jso {
            justifyContent = JustifyContent.center
            display = Display.flex
        }
        h4 {
            style = jso {
                color = "#5a5c69".unsafeCast<Color>()
            }
            fontAwesomeIcon(icon = icon)

            className = ClassName("mt-2 mb-4")
            +title
        }
    }
}

/**
 * @param selectedTab
 * @param tabsList
 * @param updateTabState
 */
fun ChildrenBuilder.tab(selectedTab: String?, tabsList: List<String>, updateTabState: (String) -> Unit) {
    div {
        className = ClassName("row")
        style = jso {
            justifyContent = JustifyContent.center
            display = Display.flex
        }

        nav {
            className = ClassName("nav nav-tabs mb-4")
            tabsList.forEachIndexed { i, value ->
                li {
                    className = ClassName("nav-item")
                    val classVal =
                            if ((i == 0 && selectedTab == null) || selectedTab == value) {
                                " active font-weight-bold"
                            } else {
                                ""
                            }
                    p {
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
