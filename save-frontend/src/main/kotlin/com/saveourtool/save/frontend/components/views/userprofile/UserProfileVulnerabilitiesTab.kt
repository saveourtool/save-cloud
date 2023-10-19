@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.views.userprofile

import com.saveourtool.save.entities.cosv.VulnerabilityMetadataDto
import com.saveourtool.save.filters.VulnerabilityFilter
import com.saveourtool.save.frontend.components.tables.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import react.*
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import web.cssom.ClassName

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val vulnerabilityTable: FC<VulnerabilityProps> = tableComponent(
    columns = {
        columns {
            column(id = "name", header = "Name", { this.identifier }) { cellContext ->
                Fragment.create {
                    td {
                        Link {
                            to = "/${FrontendRoutes.VULNERABILITY_SINGLE}/${cellContext.row.original.identifier}"
                            +cellContext.value
                        }
                    }
                }
            }
            column(id = "short_description", header = "Description", { summary }) { cellContext ->
                Fragment.create {
                    td {
                        className = ClassName("align-middle")
                        +cellContext.row.original.summary
                    }
                }
            }
            column(id = "progress", header = "Criticality", { severityNum }) { cellContext ->
                Fragment.create {
                    td {
                        className = ClassName("align-middle")
                        +"${cellContext.row.original.severityNum}"
                    }
                }
            }
            column(id = "language", header = "Language", { language }) { cellContext ->
                Fragment.create {
                    td {
                        +"${ cellContext.row.original.language }"
                    }
                }
            }
            column(id = "status", header = "Status", { status }) { cellContext ->
                Fragment.create {
                    td {
                        +"${ cellContext.row.original.status }"
                    }
                }
            }
        }
    },
    initialPageSize = 10,
    useServerPaging = true,
    isTransparentGrid = true,
    tableOptionsCustomizer = { tableOptions ->
        enableExpanding(tableOptions)
    },
) {
    arrayOf(it.count)
}

val renderVulnerabilityTableForProfileView: FC<UserProfileVulnerabilitiesTabProps> = FC { props ->

    val (countVulnerability, setCountVulnerability) = useState(0)
    val vulnerabilityFilter = VulnerabilityFilter("", null, isOwner = true)

    val enrollRequest = useDeferredRequest {
        val count: Int = post(
            url = "$apiUrl/vulnerabilities/count/by-filter",
            headers = jsonHeaders,
            body = Json.encodeToString(vulnerabilityFilter),
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        ).unsafeMap {
            it.decodeFromJsonString()
        }
        setCountVulnerability(count)
    }

    vulnerabilityTable {
        getData = { page, size ->
            post(
                url = "$apiUrl/vulnerabilities/by-filter",
                params = jso<dynamic> {
                    this.page = page
                    this.size = size
                },
                headers = jsonHeaders,
                body = Json.encodeToString(vulnerabilityFilter),
                loadingHandler = ::loadingHandler,
                responseHandler = ::noopResponseHandler,
            ).unsafeMap {
                it.decodeFromJsonString()
            }
        }
        getPageCount = { pageSize ->
            enrollRequest()
            countVulnerability / pageSize + 1
        }
        count = countVulnerability
    }
}

/**
 * `Props` for vulnerabilities table
 */
external interface VulnerabilityProps : TableProps<VulnerabilityMetadataDto> {
    /**
     * Count of vulnerability
     */
    var count: Int?
}

/**
 * [Props] of user profile vulnerabilities tab component
 */
external interface UserProfileVulnerabilitiesTabProps : Props {
    /**
     * Name of user
     */
    var userName: String
}
