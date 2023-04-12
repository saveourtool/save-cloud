@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.save.entities.ProjectDto
import com.saveourtool.save.entities.ProjectProblemDto
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.info.UserInfo

import csstype.ClassName
import react.FC
import react.Fragment
import react.Props
import react.create
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.td
import react.router.dom.Link

/**
 * SECURITY tab in ProjectView
 */
val projectSecurityMenu = projectSecurityMenu()

@Suppress(
    "TYPE_ALIAS",
    "MAGIC_NUMBER",
)
private val problemsTable: FC<TableProps<ProjectProblemDto>> = tableComponent(
    columns = {
        columns {
            column(id = "name", header = "Name", { name }) { cellContext ->
                Fragment.create {
                    td {
                        Link {
                            to = "/${cellContext.row.original.name}"
                            +cellContext.value
                        }
                    }
                }
            }
            column(id = "passed", header = "Description") { cellContext ->
                Fragment.create {
                    td {
                        +(cellContext.value.description.ifEmpty { "Description not provided" })
                    }
                }
            }
            column(id = "critical", header = "Critical") { cellContext ->
                Fragment.create {
                    td {
                        +cellContext.value.critical.toString()
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

/**
 * ProjectSecurityMenu component props
 */
external interface ProjectSecurityMenuProps : Props {
    /**
     * Current project settings
     */
    var project: ProjectDto

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo
}

private fun projectSecurityMenu() = FC<ProjectSecurityMenuProps> { props ->

    div {
        className = ClassName("row justify-content-center")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-2 mr-3")
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
                                        +"Reporting"
                                    }
                                    div {
                                        className = ClassName("menu")
                                        div {
                                            className = ClassName("mt-2")
                                            a {
                                                className = ClassName("item")
                                                fontAwesomeIcon(icon = faShieldVirus) {
                                                    it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                                                }
                                                Link {
                                                    to = "/project/${props.project.organizationName}/${props.project.name}/security"
                                                    +"Problems"
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
        }

        // ===================== RIGHT COLUMN =======================================================================
        div {
            className = ClassName("col-6")
            problemsTable {
                getData = { _, _ -> emptyArray() }
            }
        }
    }
}
