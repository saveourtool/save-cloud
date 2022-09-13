/**
 * View for tests execution history
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.externals.fontawesome.faCheck
import com.saveourtool.save.frontend.externals.fontawesome.faExclamationTriangle
import com.saveourtool.save.frontend.externals.fontawesome.faSpinner
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.themes.Colors
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.DATABASE_DELIMITER

import csstype.Background
import csstype.ClassName
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.table.columns

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.js.jso

/**
 * [Props] for tests execution history
 */
external interface HistoryProps : PropsWithChildren {
    /**
     * Project organization name
     */
    var organizationName: String

    /**
     * Project name
     */
    var name: String
}

/**
 * [State] of history view component
 */
external interface HistoryViewState : State {
    /**
     * state for the creation of unified confirmation logic
     */
    var confirmationType: ConfirmationType

    /**
     * Message of warning
     */
    var confirmMessage: String

    /**
     * Flag to handle confirm Window
     */
    var isConfirmWindowOpen: Boolean

    /**
     * Flag to handle delete execution Window
     */
    var isDeleteExecutionWindowOpen: Boolean

    /**
     * id execution
     */
    var deleteExecutionId: List<Long>

    /**
     * Label of confirm Window
     */
    var confirmLabel: String
}

/**
 * A table to display execution results for a certain project.
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class HistoryView : AbstractView<HistoryProps, HistoryViewState>(false) {
    @Suppress("MAGIC_NUMBER")
    private val executionsTable = tableComponent(
        columns = columns<ExecutionDto> {
            column("result", "", { status }) { cellProps ->
                val result = when (cellProps.row.original.status) {
                    ExecutionStatus.ERROR -> ResultColorAndIcon("text-danger", faExclamationTriangle)
                    ExecutionStatus.OBSOLETE -> ResultColorAndIcon("text-secondary", faExclamationTriangle)
                    ExecutionStatus.PENDING -> ResultColorAndIcon("text-success", faSpinner)
                    ExecutionStatus.RUNNING -> ResultColorAndIcon("text-success", faSpinner)
                    ExecutionStatus.FINISHED -> if (cellProps.row.original.failedTests != 0L) {
                        ResultColorAndIcon("text-danger", faExclamationTriangle)
                    } else {
                        ResultColorAndIcon("text-success", faCheck)
                    }
                }
                Fragment.create {
                    td {
                        a {
                            href = getHrefToExecution(cellProps.row.original.id, cellProps.row.original.status, null)
                            fontAwesomeIcon(result.resIcon, classes = result.resColor)
                        }
                    }
                }
            }
            column("status", "Status", { status }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = getHrefToExecution(cellProps.row.original.id, cellProps.row.original.status, null)
                            +"${cellProps.value}"
                        }
                    }
                }
            }
            column("startDate", "Start time", { startTime }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = getHrefToExecution(cellProps.row.original.id, cellProps.row.original.status, null)
                            +(formattingDate(cellProps.value) ?: "Starting")
                        }
                    }
                }
            }
            column("endDate", "End time", { endTime }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = getHrefToExecution(cellProps.row.original.id, cellProps.row.original.status, null)
                            +(formattingDate(cellProps.value) ?: "Starting")
                        }
                    }
                }
            }
            column("testSuiteSource", "Test Suite Source", { testSuiteSourceName }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = getHrefToExecution(cellProps.row.original.id, cellProps.row.original.status, null)
                            +"${cellProps.value}"
                        }
                    }
                }
            }
            column("contestName", "Participating in contest", { contestName ?: "N/A" }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = getHrefToExecution(cellProps.row.original.id, cellProps.row.original.status, null)
                            +cellProps.value
                        }
                    }
                }
            }
            column("running", "Running", { runningTests }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = getHrefToExecution(cellProps.row.original.id, cellProps.row.original.status, TestResultStatus.RUNNING)
                            +"${cellProps.value}"
                        }
                    }
                }
            }
            column("passed", "Passed", { passedTests }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = getHrefToExecution(cellProps.row.original.id, cellProps.row.original.status, TestResultStatus.PASSED)
                            +"${cellProps.value}"
                        }
                    }
                }
            }
            column("failed", "Failed", { failedTests }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = getHrefToExecution(cellProps.row.original.id, cellProps.row.original.status, TestResultStatus.FAILED)
                            +"${cellProps.value}"
                        }
                    }
                }
            }
            column("skipped", "Skipped", { skippedTests }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = getHrefToExecution(cellProps.row.original.id, cellProps.row.original.status, TestResultStatus.IGNORED)
                            +"${cellProps.value}"
                        }
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
                            disabled = cellProps.value.contestName != null
                            onClick = {
                                deleteExecution(cellProps.value.id)
                            }
                        }
                    }
                }
            }
        },
        getRowProps = { row ->
            val color = when (row.original.status) {
                ExecutionStatus.ERROR -> Colors.RED
                ExecutionStatus.OBSOLETE -> Colors.GREY
                ExecutionStatus.PENDING -> Colors.GREY
                ExecutionStatus.RUNNING -> Colors.GREY
                ExecutionStatus.FINISHED -> if (row.original.failedTests != 0L) Colors.DARK_RED else Colors.GREEN
            }
            jso {
                style = jso {
                    background = color.value.unsafeCast<Background>()
                }
            }
        }
    )
    init {
        state.isConfirmWindowOpen = false
        state.isDeleteExecutionWindowOpen = false
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "ForbiddenComment",
        "LongMethod",
    )
    override fun ChildrenBuilder.render() {
        displayModal(
            state.isConfirmWindowOpen,
            state.confirmLabel,
            state.confirmMessage,
            mediumTransparentModalStyle,
            { setState { isConfirmWindowOpen = false } }
        ) {
            buttonBuilder("Ok") {
                deleteExecutionsBuilder()
                setState { isConfirmWindowOpen = false }
            }
            buttonBuilder("Cancel", "secondary") {
                setState { isConfirmWindowOpen = false }
            }
        }

        displayModal(
            state.isDeleteExecutionWindowOpen,
            state.confirmLabel,
            state.confirmMessage,
            mediumTransparentModalStyle,
            { setState { isDeleteExecutionWindowOpen = false } }
        ) {
            buttonBuilder("Ok") {
                deleteExecutionBuilder(state.deleteExecutionId)
                setState { isDeleteExecutionWindowOpen = false }
            }
            buttonBuilder("Cancel", "secondary") {
                setState { isDeleteExecutionWindowOpen = false }
            }
        }

        div {
            button {
                type = ButtonType.button
                className = ClassName("btn btn-danger mb-4")
                onClick = {
                    deleteExecutions()
                }
                +"Delete all executions"
            }
        }
        executionsTable {
            tableHeader = "Executions details"
            getData = { _, _ ->
                get(
                    url = "$apiUrl/executionDtoList?name=${props.name}&organizationName=${props.organizationName}",
                    headers = jsonHeaders,
                    loadingHandler = ::classLoadingHandler
                )
                    .unsafeMap {
                        it.decodeFromJsonString<Array<ExecutionDto>>()
                    }
            }
            getPageCount = null
        }
    }

    private fun formattingDate(date: Long?) = date?.let {
        Instant.fromEpochSeconds(date, 0)
            .toString()
            .replace("[TZ]".toRegex(), " ")
    }

    private fun deleteExecutions() {
        setState {
            confirmationType = ConfirmationType.DELETE_CONFIRM
            isConfirmWindowOpen = true
            confirmLabel = ""
            confirmMessage = "Are you sure you want to delete all executions?"
        }
    }

    private fun deleteExecutionsBuilder() {
        scope.launch {
            val responseFromDeleteExecutions =
                    post(
                        "$apiUrl/execution/deleteAllExceptContest?name=${props.name}&organizationName=${props.organizationName}",
                        jsonHeaders,
                        undefined,
                        loadingHandler = ::noopLoadingHandler,
                    )

            if (responseFromDeleteExecutions.ok) {
                window.location.href = "${window.location.origin}#/${props.organizationName}/${props.name}"
            }
        }
    }

    private fun deleteExecution(id: Long) {
        setState {
            confirmationType = ConfirmationType.DELETE_CONFIRM
            isDeleteExecutionWindowOpen = true
            confirmLabel = ""
            confirmMessage = "Are you sure you want to delete this execution?"
            deleteExecutionId = listOf(id)
        }
    }

    private fun deleteExecutionBuilder(executionIds: List<Long>) {
        scope.launch {
            val responseFromDeleteExecutions =
                    post(
                        "$apiUrl/execution/delete?executionIds=${executionIds.joinToString(DATABASE_DELIMITER)}",
                        jsonHeaders,
                        undefined,
                        loadingHandler = ::noopLoadingHandler
                    )

            if (responseFromDeleteExecutions.ok) {
                window.location.reload()
            }
        }
    }

    private fun getHrefToExecution(
        id: Long,
        executionStatus: ExecutionStatus,
        status: TestResultStatus?,
    ): String = if (executionStatus == ExecutionStatus.OBSOLETE) {
        "${window.location}"
    } else {
        "${window.location}/execution/$id${status?.let { "?status=$it" } ?: ""}"
    }

    /**
     * @property resColor
     * @property resIcon
     */
    private data class ResultColorAndIcon(val resColor: String, val resIcon: dynamic)

    companion object : RStatics<HistoryProps, HistoryViewState, HistoryView, Context<RequestStatusContext>>(HistoryView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
