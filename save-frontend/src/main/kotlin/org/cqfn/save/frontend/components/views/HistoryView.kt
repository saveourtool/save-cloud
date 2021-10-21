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
import org.cqfn.save.frontend.utils.unsafeMap

import csstype.Background
import kotlinext.js.jsObject
import org.w3c.fetch.Headers
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.buildElement
import react.child
import react.dom.a
import react.dom.td
import react.table.columns

import kotlinx.browser.window
import kotlinx.datetime.Instant

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
 * A table to display execution results for a certain project.
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class HistoryView : RComponent<HistoryProps, State>() {
    @Suppress(
        "TOO_LONG_FUNCTION",
        "MAGIC_NUMBER",
        "ForbiddenComment",
        "LongMethod")
    override fun RBuilder.render() {
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

    private fun getHrefToExecution(id: Long) = "${window.location}/execution/$id"

    /**
     * @property resColor
     * @property resIcon
     */
    private data class ResultColorAndIcon(val resColor: String, val resIcon: String)
}
