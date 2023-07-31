@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.save.frontend.utils.isVuln
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.router.dom.Link
import remix.run.router.Location
import web.cssom.ClassName
import web.cssom.Width
import web.cssom.rem

/**
 * If [Location.pathname] has more slashes then [TOP_BAR_PATH_SEGMENTS_HIGHLIGHT],
 * there is no need to highlight topbar element as we have `/demo` and `/project/.../demo`
 */
private const val TOP_BAR_PATH_SEGMENTS_HIGHLIGHT = 4

@Suppress("MAGIC_NUMBER")
private val saveTopbarLinks = sequenceOf(
    TopBarLink(hrefAnchor = FrontendRoutes.DEMO.path, width = 4.rem, text = "Demo"),
    TopBarLink(hrefAnchor = "${FrontendRoutes.DEMO}/cpg", width = 3.rem, text = "CPG"),
    TopBarLink(hrefAnchor = FrontendRoutes.AWESOME_BENCHMARKS.path, width = 13.rem, text = "Awesome Benchmarks"),
    TopBarLink(hrefAnchor = FrontendRoutes.SANDBOX.path, width = 10.rem, text = "Try SAVE format"),
    TopBarLink(hrefAnchor = FrontendRoutes.PROJECTS.path, width = 9.rem, text = "Projects board"),
    TopBarLink(hrefAnchor = FrontendRoutes.CONTESTS.path, width = 6.rem, text = "Contests"),
    TopBarLink(hrefAnchor = FrontendRoutes.ABOUT_US.path, width = 6.rem, text = "About us"),
)

@Suppress("MAGIC_NUMBER")
private val vulnTopbarLinks = sequenceOf(
    TopBarLink(hrefAnchor = FrontendRoutes.CREATE_VULNERABILITY.path, width = 13.rem, text = "Propose vulnerability"),
    TopBarLink(hrefAnchor = FrontendRoutes.VULNERABILITIES.path, width = 8.rem, text = "Vulnerabilities"),
    TopBarLink(hrefAnchor = FrontendRoutes.TOP_RATING.path, width = 7.rem, text = "Top Rating"),
)

/**
 * Displays the static links that do not depend on the url.
 */
@Suppress("LongMethod", "TOO_LONG_FUNCTION")
val topBarLinks: FC<TopBarLinksProps> = FC { props ->
    ul {
        className = ClassName("navbar-nav mx-auto")
        when {
            props.location.isVuln() -> vulnTopbarLinks
            else -> saveTopbarLinks
        }
            .forEach { elem ->
                li {
                    className = ClassName("nav-item")
                    if (elem.isExternalLink) {
                        a {
                            className = ClassName("nav-link d-flex align-items-center text-light me-2 active")
                            style = jso { width = elem.width }
                            href = elem.hrefAnchor
                            +elem.text
                        }
                    } else {
                        Link {
                            className = ClassName(
                                "nav-link d-flex align-items-center me-2 ${
                                    textColor(
                                        elem.hrefAnchor,
                                        props.location
                                    )
                                } active"
                            )
                            style = jso { width = elem.width }
                            to = elem.hrefAnchor
                            +elem.text
                        }
                    }
                }
            }
    }
}

/**
 * [Props] of the top bar links component
 */
external interface TopBarLinksProps : Props {
    /**
     * The location is needed to change the color of the text.
     */
    var location: Location
}

/**
 * @property hrefAnchor the link
 * @property width the width of the link text
 * @property text the link text
 * @property isExternalLink
 */
data class TopBarLink(
    val hrefAnchor: String,
    val width: Width,
    val text: String,
    val isExternalLink: Boolean = false,
)

private fun textColor(
    hrefAnchor: String,
    location: Location,
) =
        if (location.pathname.endsWith(hrefAnchor) && location.pathname.count { it == '/' } < TOP_BAR_PATH_SEGMENTS_HIGHLIGHT) {
            "text-warning"
        } else {
            "text-light"
        }
