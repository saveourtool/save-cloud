/**
 * View for displaying individual execution results
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.frontend.components.basic.executionStatistics
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.themes.Colors
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.frontend.utils.unsafeMap

import csstype.Background
import kotlinext.js.jsObject
import org.w3c.fetch.Headers
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.buildElement
import react.dom.button
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

/**
 * [RProps] for execution results view
 */
external interface ExecutionProps : PropsWithChildren {
    /**
     * ID of execution
     */
    var executionId: String
}

/**
 * A state of execution view
 */
external interface ExecutionState : State {
    /**
     * Execution dto
     */
    var executionDto: ExecutionDto?
}

/**
 * A [RComponent] for execution view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ExecutionView : RComponent<ExecutionProps, ExecutionState>() {
    init {
        state.executionDto = null
    }

    override fun componentDidMount() {
        GlobalScope.launch {
            val headers = Headers().also { it.set("Accept", "application/json") }
            val executionDtoFromBackend: ExecutionDto =
                    get("${window.location.origin}/executionDto?executionId=${props.executionId}", headers)
                        .decodeFromJsonString()
            setState { executionDto = executionDtoFromBackend }
        }
    }

    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION", "LongMethod")
    override fun RBuilder.render() {
        div {
            div("p-2 flex-auto") {
                +("Project version: ${(state.executionDto?.version ?: "N/A")}")
            }
            div("d-flex") {
                div("p-2 mr-auto") {
                    +"Status: ${state.executionDto?.status?.toString() ?: "N/A"}"
                }
                child(executionStatistics("mr-auto")) {
                    attrs.executionDto = state.executionDto
                }
                button(classes = "btn btn-primary") {
                    +"Rerun execution"
                    attrs.onClickFunction = {
                        attrs.disabled = true
                        GlobalScope.launch {
                            post(
                                "${window.location.origin}/rerunExecution?id=${props.executionId}",
                                Headers(),
                                undefined
                            )
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
                    buildElement {
                        td {
                            +"${it.row.index}"
                        }
                    }
                }
                column(id = "startTime", header = "Start time") {
                    buildElement {
                        td {
                            +"${
                                it.value.startTimeSeconds
                                ?.let { Instant.fromEpochSeconds(it, 0) }
                                ?: "Running"
                            }"
                        }
                    }
                }
                column(id = "status", header = "Status") {
                    buildElement {
                        td {
                            +"${it.value.status}"
                        }
                    }
                }
                column(id = "path", header = "Test file path") {
                    buildElement {
                        td {
                            +it.value.filePath
                        }
                    }
                }
                column(id = "plugin", header = "Plugin type") {
                    buildElement {
                        td {
                            +it.value.pluginName
                        }
                    }
                }
                column(id = "suiteName", header = "Test suite") {
                    buildElement {
                        td {
                            +"${it.value.testSuiteName}"
                        }
                    }
                }
                column(id = "tags", header = "Tags") {
                    buildElement {
                        td {
                            +"${it.value.tags}"
                        }
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
            },
            getRowProps = { row ->
                val color = when (row.original.status) {
                    TestResultStatus.FAILED -> Colors.RED
                    TestResultStatus.IGNORED -> Colors.GOLD
                    TestResultStatus.READY, TestResultStatus.RUNNING -> Colors.GREY
                    TestResultStatus.INTERNAL_ERROR, TestResultStatus.TEST_ERROR -> Colors.DARK_RED
                    TestResultStatus.PASSED -> Colors.GREEN
                }
                jsObject {
                    style = jsObject {
                        background = color.value.unsafeCast<Background>()
                    }
                }
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
