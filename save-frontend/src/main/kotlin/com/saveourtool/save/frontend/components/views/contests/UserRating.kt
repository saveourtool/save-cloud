/**
 * Card for the rendering of ratings: for organizations and tools
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.externals.fontawesome.faArrowRight
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import csstype.*
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.p

import kotlinx.js.jso

const val NUMBER_OF_CHARACTERS_TRIMMED = 20

val userRatingFc = userRating()

/**
 * Enum that contains values for the tab that is used in rating card
 */
enum class UserRatingTab {
    ORGS, TOOLS
}

/**
 * properties for rating fc
 */
external interface UserRatingProps : Props {
    /**
     * all projects that are registered in SAVE.
     * FixMe: only TOP PUBLIC projects will be used here in future
     */
    var projects: Set<Project>

    /**
     * all organizations that are registered in SAVE.
     * FixMe: only TOP PUBLIC organizations will be used here in future
     */
    var organizations: Set<Organization>

    /**
     * string value of the selected tab: organization/tools/etc.
     */
    var selectedTab: String?

    /**
     * callback that will be passed into this fc from the view
     */
    var updateTabState: (String) -> Unit
}

private fun ChildrenBuilder.renderingProjectChampionsTable(projects: Set<Project>) {
    projects.forEachIndexed { i, project ->
        div {
            className = ClassName("row text-muted pb-3 mb-3 border-bottom border-gray mx-2")
            div {
                className = ClassName("col-lg-2")
                h3 {
                    className = ClassName("text-info")
                    +(i + 1).toString()
                }
            }

            div {
                className = ClassName("col-lg-6")
                p {
                    className = ClassName("media-body pb-3 mb-0 small lh-125 text-left")
                    ReactHTML.strong {
                        className = ClassName("d-block text-gray-dark")
                        +project.name
                    }
                    +("${project.description?.take(NUMBER_OF_CHARACTERS_TRIMMED) ?: ""}... ")
                    a {
                        href = "#/${project.url}"
                        fontAwesomeIcon(faArrowRight)
                    }
                }
            }

            // FixMe: add rating after kirill's changes
            div {
                className = ClassName("col-lg-4")
                p {
                    +"4560"
                }
            }
        }
    }
}

private fun ChildrenBuilder.renderingOrganizationChampionsTable(organizations: Set<Organization>) {
    organizations.forEachIndexed { i, organization ->
        div {
            className = ClassName("row text-muted pb-3 mb-3 border-bottom border-gray mx-2")
            div {
                className = ClassName("col-lg-2")
                h3 {
                    className = ClassName("text-info")
                    +(i + 1).toString()
                }
            }

            div {
                className = ClassName("col-lg-6")
                p {
                    className = ClassName("media-body pb-3 mb-0 small lh-125 text-left")
                    ReactHTML.strong {
                        className = ClassName("d-block text-gray-dark")
                        +organization.name
                    }
                    +("${organization.description?.take(NUMBER_OF_CHARACTERS_TRIMMED) ?: ""}... ")
                    a {
                        href = "#/${organization.name}"
                        fontAwesomeIcon(faArrowRight)
                    }
                }
            }

            // FixMe: add rating after kirill's changes
            div {
                className = ClassName("col-lg-4")
                p {
                    +"4560"
                }
            }
        }
    }
}

/**
 * @return functional component for the rating card
 */
fun userRating() = FC<UserRatingProps> { props ->
    div {
        className = ClassName("col-lg-3")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 30.rem
            }

            div {
                className = ClassName("col")

                title(" Global Rating", faTrophy)
                tab(props.selectedTab, UserRatingTab.values().map { it.name }, props.updateTabState)
                when (props.selectedTab) {
                    UserRatingTab.ORGS.name -> renderingOrganizationChampionsTable(props.organizations)
                    UserRatingTab.TOOLS.name -> renderingProjectChampionsTable(props.projects)
                }

                div {
                    className = ClassName("row")
                    style = jso {
                        justifyContent = JustifyContent.center
                        display = Display.flex
                        alignItems = AlignItems.center
                        alignSelf = AlignSelf.start
                    }

                    a {
                        className = ClassName("mb-5")
                        // FixMe: new view on this link
                        href = ""
                        +"View more "
                    }
                }
            }
        }
    }
}
