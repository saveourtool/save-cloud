@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.ProjectStatus
import com.saveourtool.save.frontend.components.basic.projects.responseChangeProjectStatus
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.components.views.*
import com.saveourtool.save.frontend.externals.fontawesome.faRedo
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.isSuperAdmin
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes
import csstype.ClassName
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.router.dom.Link

val organizationToolsMenu = organizationToolsMenu()

/**
 * OrganizationToolsMenu component props
 */
external interface OrganizationToolsMenuProps : Props {
    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo?

    /**
     * [Role] of user that is observing this component
     */
    var selfRole: Role

    /**
     * Current organization
     */
    var organization: Organization?

    /**
     * Organization projects
     */
    var projects: List<Project>

    /**
     * lambda for update projects
     */
    var updateProjects: (MutableList<Project>) -> Unit
}

@Suppress("TOO_LONG_FUNCTION")
private fun organizationToolsMenu() = FC<OrganizationToolsMenuProps> { props ->
    val (projects, setProjects) = useState(props.projects)


    val tableWithProjects: FC<TableProps<Project>> = tableComponent(
        columns = {
            columns {
                column(id = "name", header = "Evaluated Tool", { name }) { cellContext ->
                    Fragment.create {
                        val project = cellContext.row.original
                        td {
                            when (project.status) {
                                ProjectStatus.CREATED -> div {
                                    a {
                                        href = "#/${project.organization.name}/${cellContext.value}"
                                        +cellContext.value
                                    }
                                    statusSpan(cellContext.row.original)
                                }
                                ProjectStatus.DELETED -> div {
                                    className = ClassName("text-secondary")
                                    +cellContext.value
                                    statusSpan(cellContext.row.original)
                                }
                                ProjectStatus.BANNED -> div {
                                    className = ClassName("text-danger")
                                    +cellContext.value
                                    statusSpan(cellContext.row.original)
                                }
                            }
                        }
                    }
                }
                column(id = "description", header = "Description") {
                    Fragment.create {
                        td {
                            +(it.value.description ?: "Description not provided")
                        }
                    }
                }
                column(id = "rating", header = "Contest Rating") {
                    Fragment.create {
                        td {
                            +"0"
                        }
                    }
                }

                /*
                 * A "secret" possibility to delete projects (intended for super-admins).
                 */
                if (props.selfRole.isHigherOrEqualThan(Role.OWNER)) {
                    column(id = DELETE_BUTTON_COLUMN_ID, header = EMPTY_COLUMN_HEADER) { cellProps ->
                        Fragment.create {
                            td {
                                val project = cellProps.row.original
                                val projectName = project.name

                                when (project.status) {
                                    ProjectStatus.CREATED -> actionButton {
                                        title = "WARNING: About to delete this project..."
                                        errorTitle = "You cannot delete the project $projectName"
                                        message = """Are you sure you want to delete the project "$projectName"?"""
                                        clickMessage = "Also ban this project"
                                        buttonStyleBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                fontAwesomeIcon(icon = faTrashAlt, classes = actionIconClasses.joinToString(" "))
                                            }
                                        }
                                        classes = actionButtonClasses.joinToString(" ")
                                        modalButtons = { action, window, childrenBuilder ->
                                            with(childrenBuilder) {
                                                buttonBuilder(label = "Yes, delete $projectName", style = "danger", classes = "mr-2") {
                                                    action()
                                                    window.closeWindow()
                                                }
                                                buttonBuilder("Cancel") {
                                                    window.closeWindow()
                                                }
                                            }
                                        }
                                        onActionSuccess = { isBanMode ->
                                            val newProjects = projects.minus(project).plus(project.copy(status = if (isBanMode) ProjectStatus.BANNED else ProjectStatus.DELETED))
                                            setProjects(newProjects)
                                            props.updateProjects(newProjects.toMutableList())
                                        }
                                        conditionClick = props.currentUserInfo.isSuperAdmin()
                                        sendRequest = { isBanned ->
                                            val newStatus = if (isBanned) ProjectStatus.BANNED else ProjectStatus.DELETED
                                            responseChangeProjectStatus("${project.organization.name}/${project.name}", newStatus)
                                        }
                                    }
                                    ProjectStatus.DELETED -> actionButton {
                                        title = "WARNING: About to recover this project..."
                                        errorTitle = "You cannot recover the project $projectName"
                                        message = """Are you sure you want to recover the project "$projectName"?"""
                                        buttonStyleBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                fontAwesomeIcon(icon = faRedo, classes = actionIconClasses.joinToString(" "))
                                            }
                                        }
                                        classes = actionButtonClasses.joinToString(" ")
                                        modalButtons = { action, window, childrenBuilder ->
                                            with(childrenBuilder) {
                                                buttonBuilder(label = "Yes, recover $projectName", style = "warning", classes = "mr-2") {
                                                    action()
                                                    window.closeWindow()
                                                }
                                                buttonBuilder("Cancel") {
                                                    window.closeWindow()
                                                }
                                            }
                                        }
                                        onActionSuccess = { _ ->
                                            val newProjects = projects.minus(project).plus(project.copy(status = ProjectStatus.CREATED))
                                            setProjects(newProjects)
                                            props.updateProjects(newProjects.toMutableList())
                                        }
                                        conditionClick = props.currentUserInfo.isSuperAdmin()
                                        sendRequest = { _ ->
                                            responseChangeProjectStatus("${project.organization.name}/${project.name}", ProjectStatus.CREATED)
                                        }
                                    }
                                    ProjectStatus.BANNED -> actionButton {
                                        title = "WARNING: About to recover this BANNED project..."
                                        errorTitle = "You cannot recover the project $projectName"
                                        message = """Are you sure you want to recover the project "$projectName"?"""
                                        buttonStyleBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                fontAwesomeIcon(icon = faRedo, classes = actionIconClasses.joinToString(" "))
                                            }
                                        }
                                        classes = actionButtonClasses.joinToString(" ")
                                        modalButtons = { action, window, childrenBuilder ->
                                            with(childrenBuilder) {
                                                buttonBuilder(label = "Yes, unban $projectName", style = "danger", classes = "mr-2") {
                                                    action()
                                                    window.closeWindow()
                                                }
                                                buttonBuilder("Cancel") {
                                                    window.closeWindow()
                                                }
                                            }
                                        }
                                        onActionSuccess = { _ ->
                                            val newProjects = projects.minus(project).plus(project.copy(status = ProjectStatus.CREATED))
                                            setProjects(newProjects)
                                            props.updateProjects(newProjects.toMutableList())
                                        }
                                        conditionClick = props.currentUserInfo.isSuperAdmin()
                                        sendRequest = { _ ->
                                            responseChangeProjectStatus("${project.organization.name}/${project.name}", ProjectStatus.CREATED)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        useServerPaging = false,
        usePageSelection = false,
        getAdditionalDependencies = { tableProps ->
            /*-
             * Necessary for the table to get re-rendered once a project gets
             * deleted.
             *
             * The order and size of the array must remain constant.
             */
            arrayOf(tableProps)
        }
    )

    div {
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("col-6")
            div {
                className = ClassName("d-flex justify-content-center mb-2")
                if (props.selfRole.isHigherOrEqualThan(Role.ADMIN)) {
                    Link {
                        to = "/${FrontendRoutes.CREATE_PROJECT.path}/${props.organization?.name}"
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-outline-info")
                            +"Add new Tool"
                        }
                    }
                }
            }

            tableWithProjects {
                getData = { _, _ ->
                    getProjectsFromCache(projects).toTypedArray()
                }
                getPageCount = null
            }
        }
    }
}

/**
 * Small workaround to avoid the request to the backend for the second time and to use it inside the Table view
 */
private fun getProjectsFromCache(projects: List<Project>): List<Project> =
        projects.filter { it.status == ProjectStatus.CREATED } + projects.filter { it.status == ProjectStatus.DELETED } + projects.filter { it.status == ProjectStatus.BANNED }
