@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.userprofile

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.*
import react.dom.html.ReactHTML.td
import react.router.dom.Link

val renderVulnerabilityTable: FC<UserProfileVulnerabilitiesTabProps> = FC { props ->

    @Suppress(
        "TYPE_ALIAS",
        "MAGIC_NUMBER",
    )
    val vulnerabilityTable: FC<TableProps<VulnerabilityDto>> = tableComponent(
        columns = {
            columns {
                column(id = "name", header = "Name", { this.name }) { cellContext ->
                    Fragment.create {
                        td {
                            Link {
                                to = "/${FrontendRoutes.VULNERABILITIES}/${cellContext.row.original.name}"
                                +cellContext.value
                            }
                        }
                    }
                }
                column(id = "short_description", header = "Description", { progress }) { cellContext ->
                    Fragment.create {
                        td {
                            +cellContext.row.original.shortDescription
                        }
                    }
                }
                column(id = "progress", header = "Criticality", { progress }) { cellContext ->
                    Fragment.create {
                        td {
                            +"${ cellContext.row.original.progress }"
                        }
                    }
                }
            }
        },
        initialPageSize = 10,
        useServerPaging = false,
        isTransparentGrid = true,
    )

    vulnerabilityTable {
        getData = { _, _ ->
            get(
                url = "$apiUrl/vulnerabilities/by-user-and-status",
                params = jso<dynamic> {
                    userName = props.userName
                    status = VulnerabilityStatus.APPROVED
                },
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            ).unsafeMap {
                it.decodeFromJsonString()
            }
        }
    }
}

/**
 * [Props] of user profile vulnerabilities tab component
 */
external interface UserProfileVulnerabilitiesTabProps : Props {
    /**
     * User name
     */
    var userName: String
}
