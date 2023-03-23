/**
 * View for FossGraph
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.fossgraph

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityProjectDto
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.externals.progressbar.Color
import com.saveourtool.save.frontend.externals.progressbar.progressBar
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

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
import react.router.useNavigate

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Suppress(
    "MAGIC_NUMBER",
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TYPE_ALIAS",
)
val fossGraph: FC<FossGraphViewProps> = FC { props ->
    useBackground(Style.WHITE)

    val projectWindowOpenness = useWindowOpenness()

    val navigate = useNavigate()

    val (vulnerabilityProjects, setVulnerabilityProjects) = useState<Set<VulnerabilityProjectDto>>(setOf())
    val (vulnerability, setVulnerability) = useState(VulnerabilityDto.empty)
    val (isUpdateVulnerability, setIsUpdateVulnerability) = useState(false)

    val fetchProject: (VulnerabilityProjectDto) -> Unit = { project ->
        setVulnerability {
            it.copy(projects = it.projects.plus(project))
        }
        setVulnerabilityProjects {
            it.plus(project)
        }
        setIsUpdateVulnerability(true)
        projectWindowOpenness.closeWindow()
    }

    val enrollRequest = useDeferredRequest {
        val response = post(
            url = "$apiUrl/vulnerabilities/save-projects",
            headers = jsonHeaders,
            body = Json.encodeToString(vulnerabilityProjects),
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            setIsUpdateVulnerability(false)
        }
    }

    val enrollUpdateRequest = useDeferredRequest {
        val vulnerabilityUpdate = vulnerability.copy(isActive = true)
        val response = post(
            url = "$apiUrl/vulnerabilities/update",
            headers = jsonHeaders,
            body = Json.encodeToString(vulnerabilityUpdate),
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            navigate(to = "/${FrontendRoutes.FOSS_GRAPH}")
        }
    }

    val enrollDeleteRequest = useDeferredRequest {
        val response = delete(
            url = "$apiUrl/vulnerabilities/delete?name=${props.name}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            navigate(to = "/${FrontendRoutes.FOSS_GRAPH}")
        }
    }

    useRequest {
        val vulnerabilityNew = get(
            "$apiUrl/vulnerabilities/by-name-with-description?name=${props.name}",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<VulnerabilityDto>()
            }

        setVulnerability(vulnerabilityNew)
    }

    vulnerabilityProjectWindow {
        this.windowOpenness = projectWindowOpenness
        this.vulnerabilityName = vulnerabilityName
        this.fetchProjectCredentials = fetchProject
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
        props.currentUserInfo?.globalRole?.let { role ->
            if (role.isHigherOrEqualThan(Role.SUPER_ADMIN) && !vulnerability.isActive) {
                div {
                    className = ClassName("d-flex justify-content-end")
                    buttonBuilder(label = "Reject", style = "danger", classes = "mr-2") {
                        enrollDeleteRequest()
                    }
                    buttonBuilder(label = "Approve", style = "success") {
                        enrollUpdateRequest()
                    }
                }
            }
        }

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
                            rows = 8
                            disabled = true
                        }
                    }
                }
            }
            // ===================== RIGHT COLUMN =======================================================================
            div {
                className = ClassName("col-6")

                div {
                    className = ClassName("d-flex justify-content-end")
                    buttonBuilder("Add project", classes = "mr-2", isOutline = true) {
                        projectWindowOpenness.openWindow()
                    }
                    buttonBuilder(label = "Save", isDisabled = !isUpdateVulnerability) {
                        enrollRequest()
                    }
                }
                div {
                    className = ClassName("mt-5 text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Affected open source projects"
                }

                openSourceProjectTable {
                    getData = { _, _ ->
                        vulnerability.projects.filter { it.isOpenSource }.toTypedArray()
                    }
                }

                div {
                    className = ClassName("mt-5 text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Affected projects"
                }

                projectTable {
                    getData = { _, _ ->
                        vulnerability.projects.filter { !it.isOpenSource }.toTypedArray()
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

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo?
}
