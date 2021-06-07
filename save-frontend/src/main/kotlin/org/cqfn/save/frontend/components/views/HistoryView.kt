/**
 * View for tests execution history
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.tables.tableComponent
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.dom.td
import react.table.columns
import kotlin.js.json

/**
 * [RProps] for tests execution history
 */
external interface HistoryProps : RProps {
    /**
     * An active [Project]
     */
    var owner: String
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
                column("date", "Date") {
                    td {
                        +"${it.value["date"]}"
                    }
                }
                column("passed", "Passed") {
                    td {
                        +"${it.value["passed"]}"
                    }
                }
                column("failed", "Failed") {
                    td {
                        +"${it.value["failed"]}"
                    }
                }
                column("skipped", "Skipped") {
                    td {
                        +"${it.value["skipped"]}"
                    }
                }
            }
        ) { _, _ ->
            // todo: fetch data from backend using `window.location.origin`
            arrayOf(
                json("date" to "26-Jan-2016", "passed" to "26", "failed" to "17", "skipped" to "36"),
                json("date" to "76-Jun-2019", "passed" to "67", "failed" to "75", "skipped" to "236"),
            )
        }
        ) {
            attrs.tableHeader = "Execution details"
        }
    }
}
