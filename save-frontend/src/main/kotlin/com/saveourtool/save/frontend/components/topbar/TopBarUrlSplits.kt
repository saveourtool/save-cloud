@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.save.frontend.utils.TopBarUrl
import com.saveourtool.save.utils.URL_PATH_DELIMITER
import csstype.ClassName
import react.FC
import react.dom.aria.AriaCurrent
import react.dom.aria.ariaCurrent
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML
import react.router.dom.Link

val topBarUrlSplits = topBarUrlSplits()

/**
 * Displays the url and its division by "/"
 */
private fun topBarUrlSplits() = FC<TopBarPropsWithLocation> { props ->
    ReactHTML.nav {
        className = ClassName("navbar-nav mr-auto w-100")
        ariaLabel = "breadcrumb"
        ReactHTML.ol {
            className = ClassName("breadcrumb mb-0")
            ReactHTML.li {
                className = ClassName("breadcrumb-item")
                ariaCurrent = "page".unsafeCast<AriaCurrent>()
                Link {
                    to = "/"
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
                        if (url.shouldDisplayPathFragment(index)) {
                            ReactHTML.li {
                                className = ClassName("breadcrumb-item")
                                ariaCurrent = "page".unsafeCast<AriaCurrent>()
                                if (index == size - 1) {
                                    ReactHTML.a {
                                        className = ClassName("text-warning")
                                        +pathPart
                                    }
                                } else {
                                    Link {
                                        // removePrefix - to remove the # at the beginning and not rewrite the logic with the construction of the url
                                        to = url.currentPath.removePrefix("#")
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
