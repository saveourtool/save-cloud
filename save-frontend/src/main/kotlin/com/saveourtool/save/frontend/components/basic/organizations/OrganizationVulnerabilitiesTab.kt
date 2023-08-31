@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.components.views.vuln.component.uploadCosvButton
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.*
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import web.cssom.ClassName

@Suppress("MAGIC_NUMBER", "TYPE_ALIAS")
private val vulnerabilityTable: FC<TableProps<VulnerabilityDto>> = tableComponent(
    columns = {
        columns {
            column(id = "name", header = "Name", { this.name }) { cellContext ->
                Fragment.create {
                    td {
                        Link {
                            to = "/${FrontendRoutes.VULN}/${cellContext.row.original.name}"
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
            column(id = "language", header = "Language", { language }) { cellContext ->
                Fragment.create {
                    td {
                        +"${ cellContext.row.original.language }"
                    }
                }
            }
        }
    },
    initialPageSize = 10,
    useServerPaging = false,
)

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
val organizationVulnerabilitiesTab: FC<OrganizationVulnerabilitiesMenuProps> = FC { props ->
    val (vulnerabilities, setVulnerabilities) = useState<Array<VulnerabilityDto>>(emptyArray())
    useRequest {
        val fetchedVulnerabilities: Array<VulnerabilityDto> = get(
            url = "$apiUrl/vulnerabilities/by-organization-and-status",
            params = jso<dynamic> {
                organizationName = props.organizationName
                status = VulnerabilityStatus.APPROVED
            },
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        ).unsafeMap {
            it.decodeFromJsonString()
        }
        setVulnerabilities(fetchedVulnerabilities)
    }
    div {
        className = ClassName("col-8 mx-auto mt-1 mb-3")

        div {
            className = ClassName("d-flex justify-content-center mb-3")

            uploadCosvButton {
                isImage = false
            }
        }
        if (vulnerabilities.isNotEmpty()) {
            vulnerabilityTable {
                getData = { _, _ ->
                    vulnerabilities
                }
            }
        } else {
            renderTablePlaceholder("text-center p-4 bg-white", "dashed") {
                +"No vulnerabilities were found for this organization."
                if (props.isMember) {
                    br { }
                    Link {
                        to = "/vuln/create-vulnerability"
                        +"You can be the first one to create vulnerability."
                    }
                }
            }
        }
    }
}

/**
 * OrganizationVulnerabilitiesMenu component props
 */
external interface OrganizationVulnerabilitiesMenuProps : Props {
    /**
     * Current organization name
     */
    var organizationName: String

    /**
     * Flag that defines if current user can change anything in this organization
     */
    var isMember: Boolean
}
