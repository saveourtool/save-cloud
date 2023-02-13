@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.utils.SAVE_CLOUD_GITHUB_URL
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import csstype.Width
import csstype.rem
import history.Location
import js.core.jso
import react.*
import react.dom.aria.AriaRole
import react.dom.aria.ariaExpanded
import react.dom.aria.ariaLabelledBy
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.router.NavigateFunction
import react.router.dom.Link
import react.router.useNavigate

import react.router.useLocation

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

private fun ChildrenBuilder.demoDropdownEntry(
    name: String,
    href: String,
    location: Location,
    navigate: NavigateFunction,
    deactivateDropdown: () -> Unit,
) {
    dropdownEntry(null, name, location.pathname.endsWith(href)) { attrs ->
        attrs.onClick = {
            deactivateDropdown()
            navigate(to = href)
        }
    }
}

/**
 * Displays the static links that do not depend on the url.
 */
@Suppress("MAGIC_NUMBER", "LongMethod", "TOO_LONG_FUNCTION")
private fun topBarLinks() = FC<TopBarLinksProps> { props ->
    val navigate = useNavigate()
    val location = useLocation()
    var isDemoDropdownActive by useState(false)
    val deactivateDropdown = { isDemoDropdownActive = false }
    val (demos, setDemos) = useState(listOf<DemoDto>())

    useRequest {
        val fetchedDemos: List<DemoDto> = get(
            url = "$demoApiUrl/all",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        // TODO: takes only 3 values to avoid a long list of demo, needs to be revised
        setDemos(fetchedDemos.take(3))
    }

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

                demoDropdownEntry(
                    "Diktat",
                    "/${FrontendRoutes.DEMO.path}/diktat",
                    location,
                    navigate,
                    deactivateDropdown,
                )
                demoDropdownEntry(
                    "CPG",
                    "/${FrontendRoutes.DEMO.path}/cpg",
                    location,
                    navigate,
                    deactivateDropdown,
                )
                demos.forEach { demo ->
                    demoDropdownEntry(
                        "${demo.projectCoordinates}",
                        "/${FrontendRoutes.DEMO.path}/${demo.projectCoordinates.organizationName}/${demo.projectCoordinates.projectName}",
                        location,
                        navigate,
                        deactivateDropdown,
                    )
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
