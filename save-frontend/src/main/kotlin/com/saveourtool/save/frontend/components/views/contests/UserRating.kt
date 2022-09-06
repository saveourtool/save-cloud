/**
 * Card for the rendering of ratings: for organizations and tools
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.externals.fontawesome.faArrowRight
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes

import csstype.*
import react.*
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.p

import kotlinx.js.jso

const val NUMBER_OF_CHARACTERS_TRIMMED = 20

val userRating = userRating()

/**
 * Enum that contains values for the tab that is used in rating card
 */
enum class UserRatingTab {
    ORGS,
    TOOLS,
    ;

    companion object : TabMenuBar<UserRatingTab> {
        // The string is the postfix of a [regexForUrlClassification] for parsing the url
        private val postfixInRegex = values().map { it.name.lowercase() }.joinToString("|")
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: UserRatingTab = UserRatingTab.ORGS
        override val regexForUrlClassification = Regex("/${FrontendRoutes.CONTESTS_GLOBAL_RATING.path}/($postfixInRegex)")
        override fun valueOf(elem: String): UserRatingTab = UserRatingTab.valueOf(elem)
        override fun values(): Array<UserRatingTab> = UserRatingTab.values()
        override fun isNotAvailableWithThisRole(role: Role, elem: UserRatingTab?, isOrganizationCanCreateContest: Boolean?): Boolean = false
    }
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
@Suppress("TOO_LONG_FUNCTION")
private fun userRating() = VFC {
    val (selectedTab, setSelectedTab) = useState(UserRatingTab.ORGS)

    val (organizations, setOrganizations) = useState<Set<Organization>>(emptySet())
    useRequest {
        val organizationsFromBackend: List<Organization> = post(
            url = "$apiUrl/organizations/not-deleted",
            headers = jsonHeaders,
            body = undefined,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()
        setOrganizations(organizationsFromBackend.toSet())
    }

    val (projects, setProjects) = useState<Set<Project>>(emptySet())
    useRequest {
        val projectsFromBackend: List<Project> = post(
            url = "$apiUrl/projects/not-deleted",
            headers = jsonHeaders,
            body = undefined,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()
        setProjects(projectsFromBackend.toSet())
    }

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
                tab(selectedTab.name, UserRatingTab.values().map { it.name }) {
                    setSelectedTab(UserRatingTab.valueOf(it))
                }
                when (selectedTab) {
                    UserRatingTab.ORGS -> renderingOrganizationChampionsTable(organizations)
                    UserRatingTab.TOOLS -> renderingProjectChampionsTable(projects)
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
                        href = "#/${FrontendRoutes.CONTESTS_GLOBAL_RATING.path}/${selectedTab.name.lowercase()}"
                        +"View more "
                    }
                }
            }
        }
    }
}
