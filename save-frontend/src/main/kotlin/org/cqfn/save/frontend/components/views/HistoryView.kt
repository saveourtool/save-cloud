/**
 * View for tests execution history
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.unsafeMap

import org.w3c.fetch.Headers
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.dom.a
import react.dom.td
import react.table.TableRowProps
import react.table.columns

import kotlinx.browser.window
import kotlinx.datetime.Instant

/**
 * [RProps] for tests execution history
 */
external interface HistoryProps : RProps {
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
class HistoryView : RComponent<HistoryProps, RState>() {
    @Suppress(
        "TOO_LONG_FUNCTION",
        "MAGIC_NUMBER",
        "ForbiddenComment",
        "LongMethod")
    override fun RBuilder.render() {
        child(tableComponent(
            columns = columns {
                column("result", "") {
                    val isCrashed = it.value.status == ExecutionStatus.ERROR
                    val color = if (isCrashed || it.value.failedTests > 0L) "text-danger" else "text-success"
                    val icon = if (isCrashed || it.value.failedTests > 0L) "exclamation-triangle" else "check"
                    td {
                        a(href = getHrefToExecution(it.value.id)) {
                            fontAwesomeIcon(icon, classes = color)
                        }
                    }
                }
                column("status", "Status") {
                    td {
                        a(href = getHrefToExecution(it.value.id)) {
                            +"${it.value.status}"
                        }
                    }
                }
                column("date", "Date") {
                    td {
                        a(href = getHrefToExecution(it.value.id)) {
                            +(it.value.endTime?.let {
                                Instant.fromEpochSeconds(it, 0)
                                    .toString()
                                    .replace("[TZ]".toRegex(), " ")
                            } ?: "RUNNING")
                        }
                    }
                }
                column("passed", "Passed") {
                    td {
                        a(href = getHrefToExecution(it.value.id)) {
                            +"${it.value.passedTests}"
                        }
                    }
                }
                column("failed", "Failed") {
                    td {
                        a(href = getHrefToExecution(it.value.id)) {
                            +"${it.value.failedTests}"
                        }
                    }
                }
                column("skipped", "Skipped") {
                    td {
                        a(href = getHrefToExecution(it.value.id)) {
                            +"${it.value.skippedTests}"
                        }
                    }
                }
            },
            getRowProps = { row ->
                val tmp = row.original
                val isCrashed = tmp.status == ExecutionStatus.ERROR
                // fixme: since kotlin-js-wrappers pre-210 `style` is mutable in TableRowProps, should use `jsObject<TableRowProps>` here
                val props = if (isCrashed || tmp.failedTests > 0L) {
                    js("({ style: { background: 'rgba(245, 50, 50, 0.1)' } })")
                } else {
                    js("({ style: { background: 'rgba(139, 237, 78, 0.1)' } })")
                }
                props.unsafeCast<TableRowProps>()
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

    private fun getHrefToExecution(id: Long) = "${window.location}/$id"
}
