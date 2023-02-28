/**
 * Card for the rendering of ratings: for organizations and tools
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.OrganizationWithRating
import com.saveourtool.save.entities.ProjectDto
import com.saveourtool.save.filters.OrganizationFilters
import com.saveourtool.save.filters.ProjectFilters
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes

import csstype.*
import js.core.jso
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        private val postfixInRegex = values().joinToString("|") { it.name.lowercase() }
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: UserRatingTab = UserRatingTab.ORGS
        override val regexForUrlClassification = "/${FrontendRoutes.CONTESTS_GLOBAL_RATING.path}/($postfixInRegex)"
        override fun valueOf(elem: String): UserRatingTab = UserRatingTab.valueOf(elem)
        override fun values(): Array<UserRatingTab> = UserRatingTab.values()
    }
}

private fun ChildrenBuilder.renderingProjectChampionsTable(projects: Set<ProjectDto>) {
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
                    strong {
                        className = ClassName("d-block text-gray-dark")
                        a {
                            href = "#/${project.url}"
                            +project.name
                        }
                    }
                    +("${project.description} ")
                }
            }

            div {
                className = ClassName("col-lg-4")
                p {
                    +project.contestRating.toFixedStr(2)
                }
            }
        }
    }
}

private fun ChildrenBuilder.renderingOrganizationChampionsTable(organizations: Set<OrganizationWithRating>) {
    organizations.forEachIndexed { i, organizationWithRating ->
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
                    with(organizationWithRating.organization) {
                        strong {
                            className = ClassName("d-block text-gray-dark")
                            a {
                                href = "#/$name"
                                +name
                            }
                        }
                        +"$description "
                    }
                }
            }

            div {
                className = ClassName("col-lg-4")
                p {
                    +organizationWithRating.globalRating.toFixedStr(2)
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

    val (organizationsWithRating, setOrganizationsWithRating) = useState<Set<OrganizationWithRating>>(emptySet())
    useRequest {
        val organizationsFromBackend: List<OrganizationWithRating> = post(
            url = "$apiUrl/organizations/by-filters-with-rating",
            headers = jsonHeaders,
            body = Json.encodeToString(OrganizationFilters.created),
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()
        setOrganizationsWithRating(organizationsFromBackend.toSet())
    }

    val (projects, setProjects) = useState(emptySet<ProjectDto>())
    useRequest {
        val projectsFromBackend: List<ProjectDto> = post(
            url = "$apiUrl/projects/by-filters",
            headers = jsonHeaders,
            body = Json.encodeToString(ProjectFilters.created),
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()
        setProjects(projectsFromBackend.toSet())
    }

    div {
        className = ClassName("col-lg-4")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 40.rem
            }

            div {
                className = ClassName("col")

                title(" Global Rating", faTrophy)
                tab(selectedTab.name, UserRatingTab.values().map { it.name }) {
                    setSelectedTab(UserRatingTab.valueOf(it))
                }
                when (selectedTab) {
                    UserRatingTab.ORGS -> renderingOrganizationChampionsTable(organizationsWithRating)
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
