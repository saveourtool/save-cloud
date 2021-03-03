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

external interface ExecutionProps : RProps {
    var executionId: String
}

@OptIn(ExperimentalJsExport::class)
@JsExport
class ExecutionView : RComponent<ExecutionProps, RState>() {
    override fun RBuilder.render() {
        child(tableComponent(
            data = arrayOf(
                json("name" to "test 1", "result" to "passed"),
                json("name" to "test 2", "result" to "failed"),
                json("name" to "test 3", "result" to "skipped"),
                json("name" to "test 4", "result" to "failed"),
            ),
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
            }
        )) {

        }
    }
}
