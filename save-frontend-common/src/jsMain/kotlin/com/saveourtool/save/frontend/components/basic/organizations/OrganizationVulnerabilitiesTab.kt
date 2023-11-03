@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.entities.cosv.VulnerabilityMetadataDto
import com.saveourtool.save.filters.VulnerabilityFilter
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.columns
import com.saveourtool.save.frontend.components.tables.enableExpanding
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.tables.value
import com.saveourtool.save.frontend.components.views.vuln.component.uploadCosvButton
import com.saveourtool.save.frontend.externals.i18next.useTranslation
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes.VULNERABILITY_SINGLE
import js.core.jso
import react.*
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import web.cssom.ClassName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val vulnerabilitiesUrl = "$apiUrl/vulnerabilities"

private val vulnerabilityTable: FC<VulnerabilityTableProps> = tableComponent(
    columns = {
        columns {
            column(id = "name", header = "Name", VulnerabilityMetadataDto::identifier) { cellContext ->
                Fragment.create {
                    td {
                        Link {
                            val identifier = cellContext.value
                            to = "/$VULNERABILITY_SINGLE/$identifier"
                            +identifier
                        }
                    }
                }
            }
            column(id = "short_description", header = "Description", VulnerabilityMetadataDto::summary) { cellContext ->
                Fragment.create {
                    td {
                        +cellContext.value
                    }
                }
            }
            column(id = "progress", header = "Criticality", VulnerabilityMetadataDto::severityNum) { cellContext ->
                Fragment.create {
                    td {
                        +cellContext.value.toString()
                    }
                }
            }
            column(id = "language", header = "Language", VulnerabilityMetadataDto::language) { cellContext ->
                Fragment.create {
                    td {
                        +cellContext.value.toString()
                    }
                }
            }
        }
    },
    initialPageSize = 10,
    useServerPaging = true,
    tableOptionsCustomizer = ChildrenBuilder::enableExpanding
) { props ->
    arrayOf(props.count)
}

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
val organizationVulnerabilitiesTab: FC<OrganizationVulnerabilitiesMenuProps> = FC { props ->
    val filter = VulnerabilityFilter.approved.copy(
        organizationName = props.organizationName,
    )
    val filterJson = Json.encodeToString(filter)

    val (t) = useTranslation("organization")
    var count by useState(initialValue = 0)
    useRequest {
        count = post(
            url = "$vulnerabilitiesUrl/count/by-filter",
            headers = jsonHeaders,
            body = filterJson,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        ).unsafeMap { response ->
            response.decodeFromJsonString()
        }
    }
    div {
        className = ClassName("col-8 mx-auto mt-1 mb-3")

        div {
            className = ClassName("d-flex justify-content-center mb-3")

            uploadCosvButton {
                isImage = false
            }
        }
        if (count != 0) {
            vulnerabilityTable {
                getData = { pageIndex, pageSize ->
                    post(
                        url = "$vulnerabilitiesUrl/by-filter",
                        params = jso<dynamic> {
                            page = pageIndex
                            size = pageSize
                        },
                        headers = jsonHeaders,
                        body = filterJson,
                        loadingHandler = ::loadingHandler,
                        responseHandler = ::noopResponseHandler,
                    ).unsafeMap { response ->
                        response.decodeFromJsonString()
                    }
                }
                getPageCount = { pageSize ->
                    pageCount(count, pageSize)
                }
                this.count = count
            }
        } else {
            renderTablePlaceholder("text-center p-4 bg-white", "dashed") {
                +"No vulnerabilities were found for this organization.".t()
                if (props.isMember) {
                    br { }
                    Link {
                        to = "/vuln/create-vulnerability"
                        +"You can be the first one to create vulnerability.".t()
                    }
                }
            }
        }
    }
}

/**
 * The properties of [vulnerabilityTable].
 *
 * @see vulnerabilityTable
 */
external interface VulnerabilityTableProps : TableProps<VulnerabilityMetadataDto> {
    /**
     * The total count of approved vulnerabilities for this organization.
     */
    var count: Int
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

/**
 * @return the page count, using the [total] number of items and the [pageSize]
 *   items per page.
 */
private fun pageCount(total: Int, pageSize: Int): Int {
    require(total >= 0) {
        "total should be non-negative: $total"
    }
    require(pageSize > 0) {
        "pageSize should be positive: $pageSize"
    }

    /*-
     * Source: "Number Conversion" by Roland Backhouse
     * (http://www.cs.nott.ac.uk/~psarb2/G51MPC/slides/NumberLogic.pdf).
     *
     * This shouldn't be simplified: otherwise, if `total` is 0,
     * the page count of 1 would still be returned.
     */
    return (total + pageSize - 1) / pageSize
}
