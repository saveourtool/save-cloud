@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.save.entities.ProjectProblemDto
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.markdown
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.validation.FrontendRoutes
import csstype.BorderRadius
import csstype.ClassName
import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.span
import react.router.dom.Link
import react.useState

val projectProblem: FC<ProjectProblemViewProps> = FC {props ->

    val (projectProblem, setProjectProblem) = useState(ProjectProblemDto.empty)

    useRequest {
        val projectProblemNew: ProjectProblemDto = get(
            url = "$apiUrl/projects/problem/get/by-id?id=${props.projectProblemId}",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }

        setProjectProblem(projectProblemNew)
    }

    val columnCard = cardComponent(isBordered = true, hasBg = true, isNoPadding = false, isPaddingBottomNull = true)

    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mb-4")
        h1 {
            className = ClassName("h3 mb-0 text-gray-800")
            +projectProblem.name
        }
        span {
            className = ClassName("border border-danger ml-2 pr-1 pl-1 text-red-700")
            style = jso {
                borderRadius = "2em".unsafeCast<BorderRadius>()
            }
            +projectProblem.critical.toString()
        }
    }

    div {
        className = ClassName("row justify-content-center")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Information"
            }

            div {
                className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0 border-secondary")
                div {
                    className = ClassName("col mr-2 pr-0 pl-0")
                    nav {
                        div {
                            className = ClassName("pl-3 ui vertical menu profile-setting")
                            form {
                                div {
                                    className = ClassName("item mt-2")
                                    div {
                                        className = ClassName("header")
                                        +"Severity:"
                                    }
                                    div {
                                        className = ClassName("menu")
                                        div {
                                            className = ClassName("mt-2")
                                            span {
                                                className =
                                                        ClassName("border border-danger ml-2 pr-1 pl-1 text-red-700")
                                                style = jso {
                                                    borderRadius = "2em".unsafeCast<BorderRadius>()
                                                }
                                                +projectProblem.critical.toString()
                                            }
                                        }
                                    }
                                }
                            }
                            form {
                                div {
                                    className = ClassName("item mt-2")
                                    div {
                                        className = ClassName("header")
                                        +"CVE ID:"
                                    }
                                    div {
                                        className = ClassName("menu")
                                        div {
                                            className = ClassName("mt-2 pl-2")
                                            a {
                                                projectProblem.vulnerabilityName?.let {
                                                    Link {
                                                        to = "/${FrontendRoutes.FOSS_GRAPH}/${projectProblem.vulnerabilityName}"
                                                        +it
                                                    }
                                                } ?: +"No known CVE"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===================== RIGHT COLUMN =======================================================================
        div {
            className = ClassName("col-6")

            div {
                className = ClassName("col-12")
                div {
                    className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Description"
                }
                div {
                    className = ClassName("text-left")
                    columnCard {
                        markdown(projectProblem.description.split("\n").joinToString("\n\n"))
                    }
                }
            }
        }
    }
}

/**
 * [Props] for ProjectProblemView
 */
external interface ProjectProblemViewProps : Props {
    /**
     * Name of security vulnerabilities
     */
    var organizationName: String

    /**
     * Information about current user
     */
    var projectName: String

    /**
     * Information about project problem
     */
    var projectProblemId: Long
}
