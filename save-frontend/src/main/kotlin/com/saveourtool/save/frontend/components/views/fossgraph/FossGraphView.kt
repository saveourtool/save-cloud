/**
 * View for FossGraph
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.fossgraph

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityProjectDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityProjectType
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.progressbar.Color
import com.saveourtool.save.frontend.externals.progressbar.progressBar
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.textarea
import react.router.dom.Link
import react.router.useNavigate
import web.cssom.AlignItems
import web.cssom.BorderRadius
import web.cssom.ClassName
import web.cssom.Display

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val FOR_GREEN = 34
private const val FOR_YELLOW = 67

@Suppress(
    "MAGIC_NUMBER",
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TYPE_ALIAS",
)
val fossGraph: FC<FossGraphViewProps> = FC { props ->
    useBackground(Style.WHITE)

    val projectWindowOpenness = useWindowOpenness()
    val deleteVulnerabilityWindowOpenness = useWindowOpenness()

    val navigate = useNavigate()

    val (vulnerabilityProjects, setVulnerabilityProjects) = useState<Set<VulnerabilityProjectDto>>(setOf())
    val (vulnerability, setVulnerability) = useState(VulnerabilityDto.empty)
    val (isUpdateVulnerability, setIsUpdateVulnerability) = useState(false)
    val (user, setUser) = useState(props.currentUserInfo)

    val (deleteProject, setDeleteProject) = useState<VulnerabilityProjectDto?>(null)

    val fetchProject: (VulnerabilityProjectDto) -> Unit = { project ->
        setVulnerability {
            it.copy(projects = it.projects.plus(project.copy(vulnerabilityName = it.name)))
        }
        setVulnerabilityProjects {
            it.plus(project.copy(vulnerabilityName = vulnerability.name))
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
            url = "$apiUrl/vulnerabilities/approve",
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

    val enrollDeleteProjectRequest = useDeferredRequest {
        deleteProject?.let {project ->
            val response = delete(
                url = "$apiUrl/vulnerabilities/delete-project?projectName=${project.name}&vulnerabilityName=${props.name}",
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
            )
            if (response.ok) {
                setVulnerability { it.copy(projects = it.projects.minus(project)) }
            }
        }
    }

    useRequest {
        val vulnerabilityNew: VulnerabilityDto = get(
            url = "$apiUrl/vulnerabilities/by-name-with-description?name=${props.name}",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }

        setVulnerability(vulnerabilityNew)

        val userInfo: UserInfo = get(
            url = "$apiUrl/users/${props.currentUserInfo?.name}",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .decodeFromJsonString()

        setUser(userInfo)
    }

    vulnerabilityProjectWindow {
        this.windowOpenness = projectWindowOpenness
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
                column(id = "versions", header = "Versions", { versions }) { cellContext ->
                    Fragment.create {
                        td {
                            +cellContext.value
                        }
                    }
                }
                column("delete", "") { cellProps ->
                    Fragment.create {
                        td {
                            div {
                                className = ClassName("d-flex justify-content-end")
                                buttonBuilder(faTrashAlt, style = "") {
                                    setDeleteProject(value = cellProps.row.original)
                                    deleteVulnerabilityWindowOpenness.openWindow()
                                }
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
                column(id = "versions", header = "Versions", { versions }) { cellContext ->
                    Fragment.create {
                        td {
                            +cellContext.value
                        }
                    }
                }
                column("delete", "") { cellProps ->
                    Fragment.create {
                        td {
                            div {
                                className = ClassName("d-flex justify-content-end")
                                buttonBuilder(faTrashAlt, style = "") {
                                    setDeleteProject(value = cellProps.row.original)
                                    deleteVulnerabilityWindowOpenness.openWindow()
                                }
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

    displayModal(
        deleteVulnerabilityWindowOpenness.isOpen(),
        "Deletion of ${deleteProject?.let {
            "project"
        } ?: "vulnerability"}",
        "Are you sure you want to remove this ${deleteProject?.let {
            "project"
        } ?: "vulnerability"}?",
        mediumTransparentModalStyle,
        deleteVulnerabilityWindowOpenness.closeWindowAction(),
    ) {
        buttonBuilder("Ok") {
            deleteProject?.let {
                enrollDeleteProjectRequest()
            } ?: enrollDeleteRequest()
            deleteVulnerabilityWindowOpenness.closeWindow()
        }
        buttonBuilder("Close", "secondary") {
            deleteVulnerabilityWindowOpenness.closeWindow()
        }
    }

    div {
        className = ClassName("card card-body mt-0")

        val isSuperAdmin = props.currentUserInfo?.globalRole?.isHigherOrEqualThan(Role.SUPER_ADMIN) == true
        val isOwner = user?.id == vulnerability.userId
        div {
            className = ClassName("d-flex justify-content-end")

            if (isSuperAdmin || isOwner) {
                buttonBuilder(label = "Delete", style = "danger", classes = "mr-2") {
                    setDeleteProject(null)
                    deleteVulnerabilityWindowOpenness.openWindow()
                }
            }
            if (isSuperAdmin && !vulnerability.isActive) {
                buttonBuilder(label = "Approve", style = "success") {
                    enrollUpdateRequest()
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
                    val color = if (progress < FOR_GREEN) {
                        Color.GREEN.hexColor
                    } else if (progress < FOR_YELLOW) {
                        Color.YELLOW.hexColor
                    } else {
                        Color.RED.hexColor
                    }
                    progressBar(progress, color = color)

                    div {
                        className = ClassName("menu text-right")
                        div {
                            className = ClassName("mt-2")
                            span {
                                className =
                                        ClassName("border border-danger ml-2 pr-1 pl-1 text-red-700")
                                style = jso {
                                    borderRadius = "2em".unsafeCast<BorderRadius>()
                                }
                                +vulnerability.language.value
                            }
                        }
                    }
                }
                div {
                    className = ClassName("card shadow mb-4")

                    div {
                        className = ClassName("card-body")
                        textarea {
                            className = ClassName("auto_height form-control-plaintext pt-0 pb-0")
                            value = vulnerability.shortDescription
                            rows = 2
                            disabled = true
                        }
                    }

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
                                +"Related link"
                            }
                        }
                    }
                    div {
                        className = ClassName("card-body")
                        Link {
                            to = "${vulnerability.relatedLink}"
                            +"${vulnerability.relatedLink}"
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
                        vulnerability.projects.filter { it.type == VulnerabilityProjectType.PROJECT }.toTypedArray()
                    }
                }

                div {
                    className = ClassName("mt-5 text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Affected libraries"
                }

                projectTable {
                    getData = { _, _ ->
                        vulnerability.projects.filter { it.type == VulnerabilityProjectType.LIBRARY }.toTypedArray()
                    }
                }

                div {
                    className = ClassName("mt-5 text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Commits with fix"
                }

                projectTable {
                    getData = { _, _ ->
                        vulnerability.projects.filter { it.type == VulnerabilityProjectType.COMMIT }.toTypedArray()
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
