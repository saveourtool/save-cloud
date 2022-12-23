@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.save.utils.SAVE_CLOUD_GITHUB_URL
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import csstype.Width
import csstype.rem
import history.Location
import js.core.jso
import react.FC
import react.Props
import react.dom.aria.AriaRole
import react.dom.aria.ariaExpanded
import react.dom.aria.ariaLabelledBy
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.router.dom.Link
import react.router.useNavigate
import react.useState

import kotlinx.browser.window

val topBarLinks = topBarLinks()

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

/**
 * Displays the static links that do not depend on the url.
 */
@Suppress("MAGIC_NUMBER", "LongMethod", "TOO_LONG_FUNCTION")
private fun topBarLinks() = FC<TopBarLinksProps> { props ->
    val navigate = useNavigate()
    var isDemoDropdownActive by useState(false)

    ul {
        className = ClassName("navbar-nav mx-auto")
        li {
            className = ClassName("nav-item dropdown no-arrow")
            style = jso {
                width = 5.rem
            }
            a {
                className = ClassName("nav-link dropdown-toggle text-light")
                asDynamic()["data-toggle"] = "dropdown"
                ariaExpanded = false
                id = "demoDropdown"
                role = "button".unsafeCast<AriaRole>()
                +"Demo"
                onClickCapture = { _ ->
                    isDemoDropdownActive = !isDemoDropdownActive
                }
            }
            div {
                className = ClassName("dropdown-menu dropdown-menu-right shadow animated--grow-in${if (isDemoDropdownActive) " show" else "" }")
                ariaLabelledBy = "demoDropdown"
                val diktatDemoHref = "/${FrontendRoutes.DEMO.path}/diktat"
                dropdownEntry(null, "Diktat", window.location.href.contains(diktatDemoHref)) { attrs ->
                    attrs.onClick = {
                        isDemoDropdownActive = false
                        navigate(to = diktatDemoHref)
                    }
                }
                val cpgDemoHref = "/${FrontendRoutes.DEMO.path}/cpg"
                dropdownEntry(null, "CPG", window.location.href.contains(cpgDemoHref)) { attrs ->
                    attrs.onClick = {
                        isDemoDropdownActive = false
                        navigate(to = cpgDemoHref)
                    }
                }
            }
        }
        sequenceOf(
            TopBarLink(hrefAnchor = FrontendRoutes.AWESOME_BENCHMARKS.path, width = 12.rem, text = "Awesome Benchmarks"),
            TopBarLink(hrefAnchor = FrontendRoutes.SANDBOX.path, width = 9.rem, text = "Try SAVE format"),
            TopBarLink(hrefAnchor = SAVE_CLOUD_GITHUB_URL, width = 9.rem, text = "SAVE on GitHub", isExternalLink = true),
            TopBarLink(hrefAnchor = FrontendRoutes.PROJECTS.path, width = 8.rem, text = "Projects board"),
            TopBarLink(hrefAnchor = FrontendRoutes.CONTESTS.path, width = 6.rem, text = "Contests"),
            TopBarLink(hrefAnchor = FrontendRoutes.ABOUT_US.path, width = 6.rem, text = "About us"),
        ).forEach { elem ->
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
                        className = ClassName("nav-link d-flex align-items-center me-2 ${textColor(elem.hrefAnchor, props.location)} active")
                        style = jso { width = elem.width }
                        to = elem.hrefAnchor
                        +elem.text
                    }
                }
            }
        }
    }
}

private fun textColor(hrefAnchor: String, location: Location) =
        if (location.pathname.endsWith(hrefAnchor)) "text-warning" else "text-light"
