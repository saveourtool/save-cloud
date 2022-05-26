/**
 * View for tests execution history
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.frontend.components.errorStatusContext
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.externals.fontawesome.faCheck
import com.saveourtool.save.frontend.externals.fontawesome.faExclamationTriangle
import com.saveourtool.save.frontend.externals.fontawesome.faSpinner
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.themes.Colors
import com.saveourtool.save.frontend.utils.*

import csstype.Background
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.Context
import react.PropsWithChildren
import react.RBuilder
import react.RStatics
import react.State
import react.StateSetter
import react.buildElement
import react.dom.a
import react.dom.button
import react.dom.div
import react.dom.td
import react.setState
import react.table.columns

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import kotlinx.js.jso

/**
 * [RProps] for tests execution history
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
    var isConfirmWindowOpen: Boolean?

    /**
     * Flag to handle delete execution Window
     */
    var isDeleteExecutionWindowOpen: Boolean?

    /**
     * id execution
     */
    var deleteExecutionId: List<Long>

    /**
     * Label of confirm Window
     */
    var confirmLabel: String

    /**
     * Message of error
     */
    var errorMessage: String

    /**
     * Error label
     */
    var errorLabel: String
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
                    ExecutionStatus.PENDING -> ResultColorAndIcon("text-success", faSpinner)
                    ExecutionStatus.RUNNING -> ResultColorAndIcon("text-success", faSpinner)
                    ExecutionStatus.FINISHED -> if (cellProps.row.original.failedTests != 0L) {
                        ResultColorAndIcon("text-danger", faExclamationTriangle)
                    } else {
                        ResultColorAndIcon("text-success", faCheck)
                    }
                }
                buildElement {
                    td {
                        a(href = getHrefToExecution(cellProps.row.original.id, null)) {
                            fontAwesomeIcon(result.resIcon, classes = result.resColor)
                        }
                    }
                }
            }
            column("status", "Status", { status }) { cellProps ->
                buildElement {
                    td {
                        a(href = getHrefToExecution(cellProps.row.original.id, null)) {
                            +"${cellProps.value}"
                        }
                    }
                }
            }
            column("startDate", "Start time", { startTime }) { cellProps ->
                buildElement {
                    td {
                        a(href = getHrefToExecution(cellProps.row.original.id, null)) {
                            +(formattingDate(cellProps.value) ?: "Starting")
                        }
                    }
                }
            }
            column("endDate", "End time", { endTime }) { cellProps ->
                buildElement {
                    td {
                        a(href = getHrefToExecution(cellProps.row.original.id, null)) {
                            +(formattingDate(cellProps.value) ?: "Starting")
                        }
                    }
                }
            }
            column("running", "Running", { runningTests }) { cellProps ->
                buildElement {
                    td {
                        a(href = getHrefToExecution(cellProps.row.original.id, TestResultStatus.RUNNING)) {
                            +"${cellProps.value}"
                        }
                    }
                }
            }
            column("passed", "Passed", { passedTests }) { cellProps ->
                buildElement {
                    td {
                        a(href = getHrefToExecution(cellProps.row.original.id, TestResultStatus.PASSED)) {
                            +"${cellProps.value}"
                        }
                    }
                }
            }
            column("failed", "Failed", { failedTests }) { cellProps ->
                buildElement {
                    td {
                        a(href = getHrefToExecution(cellProps.row.original.id, TestResultStatus.FAILED)) {
                            +"${cellProps.value}"
                        }
                    }
                }
            }
            column("skipped", "Skipped", { skippedTests }) { cellProps ->
                buildElement {
                    td {
                        a(href = getHrefToExecution(cellProps.row.original.id, TestResultStatus.IGNORED)) {
                            +"${cellProps.value}"
                        }
                    }
                }
            }
            column("checkBox", "") { cellProps ->
                buildElement {
                    td {
                        button(type = ButtonType.button, classes = "btn btn-small") {
                            fontAwesomeIcon(icon = faTrashAlt, classes = "trash-alt")
                            attrs.onClickFunction = {
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
    ) { _, _ ->
        get(
            url = "$apiUrl/executionDtoList?name=${props.name}&organizationName=${props.organizationName}",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
        )
            .unsafeMap {
                it.decodeFromJsonString<Array<ExecutionDto>>()
            }
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "ForbiddenComment",
        "LongMethod",
    )
    override fun RBuilder.render() {
        runConfirmWindowModal(state.isConfirmWindowOpen, state.confirmLabel, state.confirmMessage, { setState { isConfirmWindowOpen = false } }) {
            deleteExecutionsBuilder()
            setState { isConfirmWindowOpen = false }
        }
        runConfirmWindowModal(state.isDeleteExecutionWindowOpen, state.confirmLabel, state.confirmMessage, { setState { isDeleteExecutionWindowOpen = false } }) {
            deleteExecutionBuilder(state.deleteExecutionId)
            setState {
                isDeleteExecutionWindowOpen = false
            }
        }
        div {
            button(type = ButtonType.button, classes = "btn btn-danger mb-4") {
                attrs.onClickFunction = {
                    deleteExecutions()
                }
                +"Delete all executions"
            }
        }
        child(executionsTable) {
            attrs.tableHeader = "Executions details"
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
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            val responseFromDeleteExecutions =
                    post("$apiUrl/execution/deleteAll?name=${props.name}&organizationName=${props.organizationName}", headers, undefined)

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
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            val responseFromDeleteExecutions =
                    post("$apiUrl/execution/delete?executionIds=${executionIds.joinToString(",")}", headers, undefined)

            if (responseFromDeleteExecutions.ok) {
                window.location.reload()
            }
        }
    }

    private fun getHrefToExecution(id: Long, status: TestResultStatus?) =
            "${window.location}/execution/$id${status?.let { "?status=$it" } ?: ""}"

    /**
     * @property resColor
     * @property resIcon
     */
    private data class ResultColorAndIcon(val resColor: String, val resIcon: dynamic)

    companion object : RStatics<HistoryProps, HistoryViewState, HistoryView, Context<StateSetter<Response?>>>(HistoryView::class) {
        init {
            contextType = errorStatusContext
        }
    }
}
