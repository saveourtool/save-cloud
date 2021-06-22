/**
 * View for tests execution history
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.unsafeMap

import org.w3c.fetch.Headers
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.dom.td
import react.table.columns

import kotlinx.browser.window
import kotlinx.coroutines.await
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
    @Suppress("TOO_LONG_FUNCTION", "ForbiddenComment")
    override fun RBuilder.render() {
        child(tableComponent(
            columns = columns {
                column("index", "#") {
                    td {
                        +"${it.row.index}"
                    }
                }
                column("status", "Status") {
                    td {
                        +"${it.value.status}"
                    }
                }
                column("date", "Date") {
                    td {
                        +(it.value.endTime?.let {
                            Instant.fromEpochSeconds(it, 0)
                                .toString()
                                .replace("[TZ]".toRegex(), " ")
                        } ?: "RUNNING")
                    }
                }
                column("passed", "Passed") {
                    td {
                        +"${it.value.passedTests}"
                    }
                }
                column("failed", "Failed") {
                    td {
                        +"${it.value.failedTests}"
                    }
                }
                column("skipped", "Skipped") {
                    td {
                        +"${it.value.skippedTests}"
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
                    it.json()
                        .await()
                        .unsafeCast<Array<ExecutionDto>>()
                }
        }
        ) {
            attrs.tableHeader = "Executions details"
        }
    }
}
