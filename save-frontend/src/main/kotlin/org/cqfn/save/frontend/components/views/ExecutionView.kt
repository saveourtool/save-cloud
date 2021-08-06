/**
 * View for displaying individual execution results
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.unsafeMap

import org.w3c.fetch.Headers
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.dom.div
import react.dom.td
import react.setState
import react.table.columns

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.frontend.utils.post
import react.dom.button

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
 * A state of execution view
 */
external interface ExecutionState : RState {
    /**
     * Execution dto
     */
    var executionDto: ExecutionDto?
}

/**
 * A [RComponent] for execution view
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class ExecutionView : RComponent<ExecutionProps, ExecutionState>() {
    init {
        state.executionDto = null
    }

    override fun componentDidMount() {
        GlobalScope.launch {
            val headers = Headers().also { it.set("Accept", "application/json") }
            val executionDtoFromBackend: ExecutionDto = get("${window.location.origin}/executionDto?executionId=${props.executionId}", headers)
                .decodeFromJsonString<ExecutionDto>()
            setState { executionDto = executionDtoFromBackend }
        }
    }

    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION")
    override fun RBuilder.render() {
        div {
            +("Project version: ${(state.executionDto?.version ?: "N/A")}")
            div(classes = "float-right") {
                +"${state.executionDto?.status ?: "Status N/A"}"
                button {
                    +"Rerun execution"
                    attrs.onClickFunction = {
                        attrs.disabled = true
                        GlobalScope.launch {
                            post("${window.location.origin}/rerunExecution?id=${props.executionId}", Headers(), null)
                        }.invokeOnCompletion {
                            window.alert("Rerun request successfully submitted")
                        }
                    }
                }
            }
        }
        // fixme: table is rendered twice because of state change when `executionDto` is fetched
        child(tableComponent(
            columns = columns {
                column(id = "index", header = "#") {
                    td {
                        +"${it.row.index}"
                    }
                }
                column(id = "startTime", header = "Start time") {
                    td {
                        +"${it.value.startTimeSeconds?.let { Instant.fromEpochSeconds(it, 0) }}"
                    }
                }
                column(id = "status", header = "Status") {
                    td {
                        +"${it.value.status}"
                    }
                }
                column(id = "path", header = "Test file path") {
                    td {
                        +it.value.filePath
                    }
                }
            },
            useServerPaging = true,
            getPageCount = { pageSize ->
                val count: Int = get(
                    url = "${window.location.origin}/testExecutionsCount?executionId=${props.executionId}",
                    headers = Headers().also {
                        it.set("Accept", "application/json")
                    },
                )
                    .json()
                    .await()
                    .unsafeCast<Int>()
                count / pageSize + 1
            }
        ) { page, size ->
            console.log("Querying test executions for page $page with size $size")
            get(
                url = "${window.location.origin}/testExecutions?executionId=${props.executionId}&page=$page&size=$size",
                headers = Headers().apply {
                    set("Accept", "application/json")
                },
            )
                .unsafeMap {
                    Json.decodeFromString<Array<TestExecutionDto>>(
                        it.text().await()
                    )
                }
        }) { }
    }
}
