@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.save.utils.SAVE_CLOUD_GITHUB
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import csstype.Width
import csstype.rem
import history.Location
import js.core.jso
import react.FC
import react.Props
import react.PropsWithChildren
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
     * Is location
     */
    var location: Location
}

/**
 * @property hrefAnchor is link
 * @property width is width of the link text
 * @property text is link text
 */
data class TopBarLink(
    val hrefAnchor: String,
    val width: Width,
    val text: String,
)

/**
 * Displays the static links that do not depend on the url
 */
@Suppress("MAGIC_NUMBER", "LongMethod", "TOO_LONG_FUNCTION")
private fun topBarLinks() = FC<TopBarLinksProps> { props ->
    val navigate = useNavigate()
    val (isDemoDropdownActive, setIsDemoDropdownActive) = useState(false)

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
                    setIsDemoDropdownActive { !it }
                }
            }
            div {
                className = ClassName("dropdown-menu dropdown-menu-right shadow animated--grow-in${if (isDemoDropdownActive) " show" else "" }")
                ariaLabelledBy = "demoDropdown"
                val diktatDemoHref = "/${FrontendRoutes.DEMO.path}/diktat"
                dropdownEntry(null, "Diktat", window.location.href.contains(diktatDemoHref)) { attrs ->
                    attrs.onClick = {
                        setIsDemoDropdownActive(false)
                        navigate(to = diktatDemoHref)
                    }
                }
                val cpgDemoHref = "/${FrontendRoutes.DEMO.path}/cpg"
                dropdownEntry(null, "CPG", window.location.href.contains(cpgDemoHref)) { attrs ->
                    attrs.onClick = {
                        setIsDemoDropdownActive(false)
                        navigate(to = cpgDemoHref)
                    }
                }
            }
        }
        listOf(
            TopBarLink(hrefAnchor = FrontendRoutes.AWESOME_BENCHMARKS.path, width = 12.rem, text = "Awesome Benchmarks"),
            TopBarLink(hrefAnchor = FrontendRoutes.SANDBOX.path, width = 9.rem, text = "Try SAVE format"),
            TopBarLink(hrefAnchor = SAVE_CLOUD_GITHUB, width = 9.rem, text = "SAVE on GitHub"),
            TopBarLink(hrefAnchor = FrontendRoutes.PROJECTS.path, width = 8.rem, text = "Projects board"),
            TopBarLink(hrefAnchor = FrontendRoutes.CONTESTS.path, width = 6.rem, text = "Contests"),
            TopBarLink(hrefAnchor = FrontendRoutes.ABOUT_US.path, width = 6.rem, text = "About us"),
        ).forEach { elem ->
            val isNotSaveCloudLink = elem.hrefAnchor != SAVE_CLOUD_GITHUB
            li {
                className = ClassName("nav-item")
                if (isNotSaveCloudLink) {
                    Link {
                        className = ClassName("nav-link d-flex align-items-center me-2 ${textColor(elem.hrefAnchor, props.location)} active")
                        style = jso { width = elem.width }
                        to = elem.hrefAnchor
                        +elem.text
                    }
                } else {
                    a {
                        className = ClassName("nav-link d-flex align-items-center me-2 active")
                        style = jso { width = elem.width }
                        href = SAVE_CLOUD_GITHUB
                        +elem.text
                    }
                }
            }
        }
    }
}

private fun textColor(hrefAnchor: String, location: Location) =
        if (location.pathname.endsWith(hrefAnchor)) "text-warning" else "text-light"
