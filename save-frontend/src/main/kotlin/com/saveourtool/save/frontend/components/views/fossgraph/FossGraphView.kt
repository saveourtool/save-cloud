/**
 * View for FossGraph
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.fossgraph

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityProjectDto
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.externals.progressbar.Color
import com.saveourtool.save.frontend.externals.progressbar.progressBar
import com.saveourtool.save.frontend.utils.*

import csstype.AlignItems
import csstype.ClassName
import csstype.Display
import js.core.get
import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.textarea
import react.router.dom.Link
import react.router.useParams

/**
 * [VFC] for foss graph view
 */
@Suppress("MAGIC_NUMBER")
val fossGraphView: VFC = VFC {
    val params = useParams()
    val vulnerabilityName = params["vulnerabilityName"]!!.toString()

    fossGraph {
        name = vulnerabilityName
    }
}

@Suppress(
    "MAGIC_NUMBER",
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TYPE_ALIAS",
)
val fossGraph: FC<FossGraphViewProps> = FC { props ->
    useBackground(Style.WHITE)

    val params = useParams()
    val vulnerabilityName = params["vulnerabilityName"]!!.toString()

    val (vulnerability, setVulnerability) = useState(VulnerabilityDto.empty)
    val (vulnerabilityProjects, setVulnerabilityProjects) = useState(emptyList<VulnerabilityProjectDto>())

    useRequest {
        val vulnerabilityNew = get(
            "$apiUrl/vulnerabilities/by-name-with-description?name=$vulnerabilityName",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<VulnerabilityDto>()
            }

        setVulnerability(vulnerabilityNew)

        val vulnerabilityProjectsNew = get(
            "$apiUrl/vulnerability-projects/by-vulnerability-name?vulnerabilityName=${vulnerabilityNew.name}",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<VulnerabilityProjectDto>>()
            }

        setVulnerabilityProjects(vulnerabilityProjectsNew)
    }

    val openSourceProjectTable: FC<TableProps<VulnerabilityProjectDto>> = tableComponent(
        columns = {
            columns {
                column(id = "name", header = "Name", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            Link {
                                to = cellContext.row.original.url
                                +cellContext.value
                            }
                        }
                    }
                }
            }
        },
        isTransparentGrid = true,
        initialPageSize = 10,
        useServerPaging = false,
        usePageSelection = false,
    )

    val projectTable: FC<TableProps<VulnerabilityProjectDto>> = tableComponent(
        columns = {
            columns {
                column(id = "name", header = "Name", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            Link {
                                to = cellContext.row.original.url
                                +cellContext.value
                            }
                        }
                    }
                }
            }
        },
        isTransparentGrid = true,
        initialPageSize = 10,
        useServerPaging = false,
        usePageSelection = false,
    )

    div {
        className = ClassName("card card-body mt-0")

        h1 {
            className = ClassName("h3 mb-0 text-center text-gray-800")
            +vulnerability.name
        }

        div {
            className = ClassName("row justify-content-center")
            // ===================== LEFT COLUMN =======================================================================
            div {
                className = ClassName("col-2 mr-3")
                div {
                    className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +""
                }
                div {
                    className = ClassName("col-xl col-md-6 mb-4")
                    val progress = vulnerability.progress
                    val color = if (progress < 51) {
                        Color.GREEN.hexColor
                    } else {
                        Color.RED.hexColor
                    }
                    progressBar(progress, color = color)
                }
                div {
                    className = ClassName("card shadow mb-4")
                    div {
                        className = ClassName("card-header py-3")
                        div {
                            className = ClassName("row")
                            h6 {
                                className = ClassName("m-0 font-weight-bold text-primary")
                                style = jso {
                                    display = Display.flex
                                    alignItems = AlignItems.center
                                }
                                +"Description"
                            }
                        }
                    }
                    div {
                        className = ClassName("card-body")
                        textarea {
                            className = ClassName("auto_height form-control-plaintext pt-0 pb-0")
                            value = "${vulnerability.description}"
                            rows = 5
                            disabled = true
                        }
                    }
                }
            }
            // ===================== RIGHT COLUMN =======================================================================
            div {
                className = ClassName("col-6")
                div {
                    className = ClassName("mt-5 text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Affected open source projects"
                }
                buttonBuilder("+") { }

                openSourceProjectTable {
                    getData = { _, _ ->
                        vulnerabilityProjects.filter { it.isOpenSource }.toTypedArray()
                    }
                }

                div {
                    className = ClassName("mt-5 text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Affected projects"
                }
                buttonBuilder("+") { }

                projectTable {
                    getData = { _, _ ->
                        vulnerabilityProjects.filter { !it.isOpenSource }.toTypedArray()
                    }
                }
            }
        }
    }
}

/**
 * [Props] for FossGraphView
 */
external interface FossGraphViewProps : Props {
    /**
     * Name of security vulnerabilities
     */
    var name: String
}
