@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.basic.contests.showContestCreationModal
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.utils.*
import csstype.ClassName
import org.w3c.fetch.Headers

import org.w3c.fetch.Response
import react.*

import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.table.columns

/**
 * CONTESTS tab in OrganizationView
 */
val organizationContestsMenu = organizationContestsMenu()

@Suppress("MAGIC_NUMBER", "TYPE_ALIAS")
private val contestsTable: FC<OrganizationContestsTableProps<ContestDto>> = tableComponent(
    columns = columns {
        column(id = "name", header = "Contest Name", { name }) { cellProps ->
            Fragment.create {
                td {
                    a {
                        href = "#/contests/${cellProps.row.original.name}"
                        +cellProps.value
                    }
                }
            }
        }
        column(id = "description", header = "Description", { description }) { cellProps ->
            Fragment.create {
                td {
                    +(cellProps.value ?: "Description is not provided")
                }
            }
        }
        column(id = "start_time", header = "Start Time", { startTime.toString() }) { cellProps ->
            Fragment.create {
                td {
                    +cellProps.value.replace("T", " ")
                }
            }
        }
        column(id = "end_time", header = "End Time", { endTime.toString() }) { cellProps ->
            Fragment.create {
                td {
                    +cellProps.value.replace("T", " ")
                }
            }
        }
    },
    initialPageSize = 10,
    useServerPaging = false,
    usePageSelection = false,
    getAdditionalDependencies = {
        arrayOf(it.isContestCreated)
    }
)

/**
 * OrganizationContestsMenu component props
 */
external interface OrganizationContestsMenuProps : Props {
    /**
     * Current organization name
     */
    var organizationName: String

    /**
     * [Role] of user that is observing this component
     */
    var selfRole: Role

    /**
     * Callback to show error message
     */
    var updateErrorMessage: (Response) -> Unit
}

/**
 * Interface for table reloading after the contest creation
 */
external interface OrganizationContestsTableProps<D : Any> : TableProps<D> {
    /**
     * Flag to update table data when contest is created
     */
    var isContestCreated: Boolean
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod"
)
private fun organizationContestsMenu() = FC<OrganizationContestsMenuProps> { props ->
    val (isToUpdateTable, setIsToUpdateTable) = useState(false)
    val (isContestCreationModalOpen, setIsContestCreationModalOpen) = useState(false)
    val refreshTable = { setIsToUpdateTable { !it } }
    showContestCreationModal(
        props.organizationName,
        isContestCreationModalOpen,
        {
            setIsContestCreationModalOpen(false)
            refreshTable()
        },
        {
            setIsContestCreationModalOpen(false)
            props.updateErrorMessage(it)
        }
    ) {
        setIsContestCreationModalOpen(false)
    }

    div {
        className = ClassName("d-flex justify-content-center mb-3")
        button {
            type = ButtonType.button
            className = ClassName("btn btn-sm btn-primary")
            disabled = !props.selfRole.hasDeletePermission()
            onClick = {
                setIsContestCreationModalOpen(true)
            }
            +"+ Create contest"
        }
    }
    div {
        className = ClassName("mb-2")
        contestsTable {
            getData = { _, _ ->
                val response = get(
                    url = "$apiUrl/contests/by-organization?organizationName=${props.organizationName}",
                    headers = Headers().also {
                        it.set("Accept", "application/json")
                    },
                    loadingHandler = ::loadingHandler,
                )
                if (response.ok) {
                    response.unsafeMap {
                        it.decodeFromJsonString<List<ContestDto>>()
                    }
                        .toTypedArray()
                } else {
                    emptyArray()
                }
            }
            isContestCreated = isToUpdateTable
        }
    }
}
