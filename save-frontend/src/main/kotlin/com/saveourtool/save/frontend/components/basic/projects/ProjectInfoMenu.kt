@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.ContestResult
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.externals.fontawesome.faCalendarAlt
import com.saveourtool.save.frontend.externals.fontawesome.faEdit
import com.saveourtool.save.frontend.externals.fontawesome.faHistory
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1
import csstype.ClassName
import kotlinx.browser.window
import kotlinx.coroutines.launch

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.fetch.Response
import react.*
import react.dom.*

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.fetch.Headers
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.figure
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul

/**
 * ProjectSettingsMenu component props
 */
external interface ProjectInfoMenuProps : Props {
    /**
     * Project name
     */
    var projectName: String

    /**
     * Organization name
     */
    var organizationName: String

    /**
     * ID of the latest execution
     */
    var latestExecutionId: Long?
}

private val infoCard = cardComponent(isBordered = true, hasBg = true)

private val contestResultCard = scoreCard()

/**
 * @return ReactElement
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod"
)
fun projectInfoMenu() = FC<ProjectInfoMenuProps> { props ->
    val (usersInProject, setUsersInProject) = useState(emptyList<UserInfo>())
    useRequest(isDeferred = false) {
        val users: List<UserInfo> = get(
            url = "$apiUrl/projects/${props.organizationName}/${props.projectName}/users",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        setUsersInProject(users)
    } ()

    val (bestResults, setBestResults) = useState(emptyList<ContestResult>())
    useRequest(isDeferred = false) {
        val results: List<ContestResult> = get(
            url = "$apiUrl/contests/${props.organizationName}/${props.projectName}/best",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        setBestResults(results)
    } ()

    val (project, setProject) = useState(Project.stub(-1))
    useRequest(isDeferred = false) {
        val projectFromBackend: Project = get(
            url = "$apiUrl/projects/get/organization-name?name=${props.projectName}&organizationName=${props.organizationName}",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        setProject(projectFromBackend)
    } ()

    div {
        className = ClassName("d-flex justify-content-between")
        div {
            className = ClassName("col-3")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Best contest scores"
            }
            ul {
                className = ClassName("list-group")
                bestResults.forEach {
                    li {
                        className = ClassName("list-group-item pl-0 pr-0 pb-0 pt-0")
                        a {
                            href = "#/contests/${it.contestName}"
                            className = ClassName("stretched-link")
                        }
                        contestResultCard {
                            name = it.contestName
                            contestScore = it.score.toDouble()
                        }
                    }
                }
            }
        }

        div {
            className = ClassName("col-3")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Members"
            }
            div {
                className = ClassName("latest-photos")
                div {
                    className = ClassName("row")
                    usersInProject.forEach {
                        div {
                            className = ClassName("col-md-4")
                            figure {
                                img {
                                    className = ClassName("img-fluid")
                                    src = it.avatar?.let { path ->
                                        "/api/$v1/avatar$path"
                                    }
                                        ?: run {
                                            "img/user.svg"
                                        }
                                    alt = ""
                                }
                            }
                        }
                    }
                }
            }
        }

        div {
            className = ClassName("col-4")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"General info"
            }
            infoCard {
                project.description?.let {
                    div {
                        className = ClassName("mt-2 d-flex justify-content-between")
                        div {
                            className = ClassName("col-5 text-left")
                            +"Tested tool description: "
                        }
                        div {
                            className = ClassName("col-7 text-left")
                            +it
                        }
                    }
                }
                project.url?.let {
                    div {
                        className = ClassName("mt-2 d-flex justify-content-between")
                        div {
                            className = ClassName("col-5 text-left")
                            +"Tested tool Url:"
                        }
                        div {
                            className = ClassName("col-7 text-left")
                            a {
                                href = it
                                +it
                            }
                        }
                    }
                }
                div {
                    className = ClassName("ml-3 mt-2 align-items-left justify-content-between")
                    fontAwesomeIcon(icon = faHistory)

                    button {
                        className = ClassName("btn btn-link text-left")
                        +"Latest Execution"
                        disabled = props.latestExecutionId == null

                        onClick = {
                            window.location.href = "${window.location}/history/execution/${props.latestExecutionId}"
                        }
                    }
                }
                div {
                    className = ClassName("ml-3 align-items-left")
                    fontAwesomeIcon(icon = faCalendarAlt)
                    a {
                        href = "#/${props.organizationName}/${props.projectName}/history"
                        className = ClassName("btn btn-link text-left")
                        +"Execution History"
                    }
                }
            }
        }
    }
}
