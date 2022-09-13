@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.entities.ContestStatus
import com.saveourtool.save.frontend.components.basic.contests.showContestCreationModal
import com.saveourtool.save.frontend.components.modal.displayConfirmationModal
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import org.w3c.fetch.Response
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.table.columns

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * CONTESTS tab in OrganizationView
 */
val organizationContestsMenu = organizationContestsMenu()

@Suppress("MAGIC_NUMBER", "TYPE_ALIAS")
private val contestsTable: FC<OrganizationContestsTableProps<ContestDto>> = tableComponent(
    columns = { props ->
        columns {
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
            column("checkBox", "") { cellProps ->
                Fragment.create {
                    td {
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-small")
                            fontAwesomeIcon(icon = faTrashAlt, classes = "trash-alt")
                            onClick = {
                                props.deleteContest(cellProps.row.original)
                            }
                        }
                    }
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

    /**
     * Fun delete contest
     */
    var deleteContest: (ContestDto) -> Unit
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
    val (deletingContest, setDeletingContest) = useState(ContestDto.empty)
    val (contests, setContests) = useState<Set<ContestDto>>(setOf())
    val refreshTable = { setIsToUpdateTable { !it } }
    val deleteContestFun = useDeferredRequest {
        val deleteContest = deletingContest.copy(status = ContestStatus.DELETED)
        val response = post(
            "$apiUrl/contests/update",
            jsonHeaders,
            Json.encodeToString(deleteContest),
            loadingHandler = ::noopLoadingHandler,
        )
        if (response.ok) {
            setContests(contests.minusElement(deletingContest))
            refreshTable()
        }
    }
    val windowOpenness = useWindowOpenness()

    useRequest {
        val response = get(
            url = "$apiUrl/contests/by-organization?organizationName=${props.organizationName}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
        val newContests = if (response.ok) {
            response.unsafeMap {
                it.decodeFromJsonString<List<ContestDto>>()
            }
                .toSet()
        } else {
            setOf()
        }
        setContests(newContests)
        refreshTable()
    }

    displayConfirmationModal(
        windowOpenness,
        "",
        "Are you sure you want to delete this contest?",
    ) {
        deleteContestFun()
    }

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
            +"Create contest"
        }
    }
    div {
        className = ClassName("mb-2")
        contestsTable {
            getData = { _, _ ->
                contests.toTypedArray()
            }
            isContestCreated = isToUpdateTable
            deleteContest = { contest ->
                setDeletingContest(contest)
                windowOpenness.openWindow()
            }
        }
    }
}
