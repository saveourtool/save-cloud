/**
 * View for displaying individual execution results
 */

package org.cqfn.save.frontend.components.views

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
 * [RProps] for execution results view
 */
external interface ExecutionProps : RProps {
    /**
     * ID of execution
     */
    var executionId: String
}

/**
 * A [RComponent] for execution view
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class ExecutionView : RComponent<ExecutionProps, RState>() {
    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
    override fun RBuilder.render() {
        child(tableComponent(
            columns = columns {
                column(id = "index", header = "#") {
                    td {
                        +"${it.row.index}"
                    }
                }
                column(id = "name", header = "Test name") {
                    td {
                        +"${it.value["name"]}"
                    }
                }
                column(id = "status", header = "Status") {
                    td {
                        +"${it.value["result"]}"
                    }
                }
            }) {
               arrayOf(
                   json("name" to "test 1", "result" to "passed"),
                   json("name" to "test 2", "result" to "failed"),
                   json("name" to "test 3", "result" to "skipped"),
                   json("name" to "test 4", "result" to "failed"),
               )
            }) { }
    }
}
