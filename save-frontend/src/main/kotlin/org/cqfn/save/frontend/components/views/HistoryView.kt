/**
 * View for tests execution history
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.themes.Colors
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.frontend.utils.runConfirmWindowModal
import org.cqfn.save.frontend.utils.runErrorModal
import org.cqfn.save.frontend.utils.unsafeMap

import csstype.Background
import kotlinext.js.jsObject
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.buildElement
import react.dom.a
import react.dom.button
import react.dom.div
import react.dom.td
import react.setState
import react.table.columns

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction

/**
 * [RProps] for tests execution history
 */
external interface HistoryProps : PropsWithChildren {
    /**
     * Project owner
     */
    var owner: String

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
     * Label of confirm Window
     */
    var confirmLabel: String

    /**
     * Message of error
     */
    var errorMessage: String

    /**
     * Flag to handle error
     */
    var isErrorOpen: Boolean?

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
class HistoryView : RComponent<HistoryProps, HistoryViewState>() {
    private lateinit var responseFromDeleteExecutions: Response

    @Suppress(
        "TOO_LONG_FUNCTION",
        "MAGIC_NUMBER",
        "ForbiddenComment",
        "LongMethod")
    override fun RBuilder.render() {
        runErrorModal(state.isErrorOpen, state.errorLabel, state.errorMessage) {
            setState { isErrorOpen = false }
        }
        runConfirmWindowModal(state.isConfirmWindowOpen, state.confirmLabel, state.confirmMessage, { setState { isConfirmWindowOpen = false } }) {
            deleteExecutionsBuilder()
            setState { isConfirmWindowOpen = false }
        }
        div {
            button(type = ButtonType.button, classes = "btn btn-danger mb-4") {
                attrs.onClickFunction = {
                    deleteExecutions()
                }
                +"Delete all executions"
            }
        }
        child(tableComponent(
            columns = columns {
                column("result", "") { cellProps ->
                    val result = when (cellProps.value.status) {
                        ExecutionStatus.ERROR -> ResultColorAndIcon("text-danger", "exclamation-triangle")
                        ExecutionStatus.PENDING -> ResultColorAndIcon("text-success", "spinner")
                        ExecutionStatus.RUNNING -> ResultColorAndIcon("text-success", "spinner")
                        ExecutionStatus.FINISHED -> if (cellProps.value.failedTests != 0L) {
                            ResultColorAndIcon("text-danger", "exclamation-triangle")
                        } else {
                            ResultColorAndIcon("text-success", "check")
                        }
                    }
                    buildElement {
                        td {
                            a(href = getHrefToExecution(cellProps.value.id)) {
                                fontAwesomeIcon(result.resIcon, classes = result.resColor)
                            }
                        }
                    }
                }
                column("status", "Status") {
                    buildElement {
                        td {
                            a(href = getHrefToExecution(it.value.id)) {
                                +"${it.value.status}"
                            }
                        }
                    }
                }
                column("date", "Date") {
                    buildElement {
                        td {
                            a(href = getHrefToExecution(it.value.id)) {
                                +(it.value.endTime?.let {
                                    Instant.fromEpochSeconds(it, 0)
                                        .toString()
                                        .replace("[TZ]".toRegex(), " ")
                                } ?: "Starting")
                            }
                        }
                    }
                }
                column("running", "Running") {
                    buildElement {
                        td {
                            a(href = getHrefToExecution(it.value.id)) {
                                +"${it.value.runningTests}"
                            }
                        }
                    }
                }
                column("passed", "Passed") {
                    buildElement {
                        td {
                            a(href = getHrefToExecution(it.value.id)) {
                                +"${it.value.passedTests}"
                            }
                        }
                    }
                }
                column("failed", "Failed") {
                    buildElement {
                        td {
                            a(href = getHrefToExecution(it.value.id)) {
                                +"${it.value.failedTests}"
                            }
                        }
                    }
                }
                column("skipped", "Skipped") {
                    buildElement {
                        td {
                            a(href = getHrefToExecution(it.value.id)) {
                                +"${it.value.skippedTests}"
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
                jsObject {
                    style = jsObject {
                        background = color.value.unsafeCast<Background>()
                    }
                }
            }
        ) { _, _ ->
            get(
                url = "${window.location.origin}/executionDtoList?name=${props.name}&owner=${props.owner}",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                    it.set("Content-Type", "application/json")
                },
            )
                .unsafeMap {
                    it.decodeFromJsonString<Array<ExecutionDto>>()
                }
        }
        ) {
            attrs.tableHeader = "Executions details"
        }
    }

    private fun deleteExecutions() {
        setState {
            confirmationType = ConfirmationType.DELETE_CONFIRM
            isConfirmWindowOpen = true
            confirmLabel = ""
            confirmMessage = "Are you sure you want to delete this project?"
        }
    }

    private fun deleteExecutionsBuilder() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        GlobalScope.launch {
            responseFromDeleteExecutions =
                    post("${window.location.origin}/deleteExecution?name=${props.name}&owner=${props.owner}", headers, undefined)
        }.invokeOnCompletion {
            if (responseFromDeleteExecutions.ok) {
                window.location.href = "${window.location.origin}#/${props.owner}/${props.name}"
            } else {
                responseFromDeleteExecutions.text().then {
                    setState {
                        errorLabel = "Failed to delete executions"
                        errorMessage = it
                        isErrorOpen = true
                    }
                }
            }
        }
    }

    private fun getHrefToExecution(id: Long) = "${window.location}/execution/$id"

    /**
     * @property resColor
     * @property resIcon
     */
    private data class ResultColorAndIcon(val resColor: String, val resIcon: String)
}
