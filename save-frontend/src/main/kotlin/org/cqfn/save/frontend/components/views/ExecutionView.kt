/**
 * View for displaying individual execution results
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.frontend.components.basic.SelectOption.Companion.ANY
import org.cqfn.save.frontend.components.basic.executionStatistics
import org.cqfn.save.frontend.components.basic.executionTestsNotFound
import org.cqfn.save.frontend.components.basic.testExecutionFiltersRow
import org.cqfn.save.frontend.components.basic.testStatusComponent
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.externals.table.useFilters
import org.cqfn.save.frontend.http.getDebugInfoFor
import org.cqfn.save.frontend.themes.Colors
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.frontend.utils.spread
import org.cqfn.save.frontend.utils.unsafeMap

import csstype.Background
import csstype.Width
import kotlinext.js.jsObject
import org.w3c.fetch.Headers
import react.table.columns
import react.table.useExpanded
import react.table.usePagination
import react.table.useSortBy

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import react.*
import react.dom.*

/**
 * [RProps] for execution results view
 */
external interface ExecutionProps : PropsWithChildren {
    /**
     * ID of execution
     */
    var executionId: String

    /**
     * Test Result Status to filter by
     */
    var status: TestResultStatus?
}

/**
 * A state of execution view
 */
external interface ExecutionState : State {
    /**
     * Execution dto
     */
    var executionDto: ExecutionDto?

    /**
     * Count tests with executionId
     */
    var countTests: Int?

    /**
     * Test Result Status to filter by
     */
    var status: TestResultStatus?

    /**
     * Name of test suite
     */
    var testSuite: String?
}

/**
 * A [RComponent] for execution view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ExecutionView : AbstractView<ExecutionProps, ExecutionState>(false) {
    init {
        state.executionDto = null
        state.status = null
        state.testSuite = null
    }

    override fun componentDidMount() {
        super.componentDidMount()

        GlobalScope.launch {
            val headers = Headers().also { it.set("Accept", "application/json") }
            val executionDtoFromBackend: ExecutionDto =
                    get("$apiUrl/executionDto?executionId=${props.executionId}", headers)
                        .decodeFromJsonString()
            val count: Int = get(
                url = "$apiUrl/testExecution/count?executionId=${props.executionId}",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                },
            )
                .json()
                .await()
                .unsafeCast<Int>()
            setState {
                executionDto = executionDtoFromBackend
                status = props.status
                countTests = count
            }
        }
    }

    @Suppress(
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "TOO_LONG_FUNCTION",
        "AVOID_NULL_CHECKS",
        "MAGIC_NUMBER",
        "ComplexMethod",
        "LongMethod"
    )
    override fun RBuilder.render() {
        div {
            div("p-2 flex-auto") {
                +("Project version: ${(state.executionDto?.version ?: "N/A")}")
            }
            div("d-flex") {
                div("p-2 mr-auto") {
                    +"Status: ${state.executionDto?.status?.name ?: "N/A"}"
                }
                child(executionStatistics("mr-auto")) {
                    attrs.executionDto = state.executionDto
                    attrs.countTests = state.countTests
                }
                div("pr-0") {
                    button(classes = "btn btn-primary") {
                        +"Rerun execution"
                        attrs.onClickFunction = {
                            attrs.disabled = true
                            GlobalScope.launch {
                                post(
                                    "$apiUrl/rerunExecution?id=${props.executionId}",
                                    Headers(),
                                    undefined
                                )
                            }.invokeOnCompletion {
                                window.alert("Rerun request successfully submitted")
                                window.location.reload()
                            }
                        }
                    }
                }
            }
        }

        div("row") {
            div("col-md-2 mb-4") {
                div("card bg-success text-white h-100 shadow py-2") {
                    div("card-body") {
                            +"""RUNNING"""
                            div("text-white-50 small") { +"""Project version: 0.0.1""" }
                    }
                }
            }

            div("col-xl-3 col-md-6 mb-4") {
                div("card border-left-info shadow h-100 py-2") {
                    div("card-body") {
                        div("row no-gutters align-items-center") {
                            div("col mr-2") {
                                div("text-xs font-weight-bold text-info text-uppercase mb-1") { +"""Pass Rate""" }
                                div("row no-gutters align-items-center") {
                                    div("col-auto") {
                                        div("h5 mb-0 mr-3 font-weight-bold text-gray-800") { +"""50%""" }
                                    }
                                    div("col") {
                                        div("progress progress-sm mr-2") {
                                            div("progress-bar bg-info") {
                                                attrs["role"] = "progressbar"
                                                attrs["style"] = kotlinext.js.jsObject<CSSProperties> {
                                                    width = "50%;".unsafeCast<Width>()
                                                }
                                                attrs["aria-valuenow"] = "50"
                                                attrs["aria-valuemin"] = "0"
                                                attrs["aria-valuemax"] = "100"
                                            }
                                        }
                                    }
                                }
                            }
                            div("col-auto") {
                                i("fas fa-clipboard-list fa-2x text-gray-300") {
                                }
                            }
                        }
                    }
                }
            }
            div("col-xl-4 col-md-6 mb-4") {
                div("card border-left-success shadow h-100 py-2") {
                    div("card-body") {
                        div("row no-gutters align-items-center") {
                            div("col mr-2") {
                                div("text-xs font-weight-bold text-success text-uppercase mb-1") { +"""Tests""" }
                                div("h5 mb-0 font-weight-bold text-gray-800") { +"""100000""" }
                            }
                            div("col mr-2") {
                                div("text-xs font-weight-bold text-success text-uppercase mb-1") { +"""Running""" }
                                div("h5 mb-0 font-weight-bold text-gray-800") { +"""543""" }
                            }
                            div("col mr-2") {
                                div("text-xs font-weight-bold text-success text-uppercase mb-1") { +"""Failed""" }
                                div("h5 mb-0 font-weight-bold text-gray-800") { +"""100000""" }
                            }
                            div("col mr-2") {
                                div("text-xs font-weight-bold text-success text-uppercase mb-1") { +"""Passed""" }
                                div("h5 mb-0 font-weight-bold text-gray-800") { +"""9000""" }
                            }
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
                            +"${it.row.index + 1 + it.state.pageIndex * it.state.pageSize}"
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
                column(id = "endTime", header = "End time") {
                    buildElement {
                        td {
                            +"${
                                it.value.endTimeSeconds
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
                column(id = "missing", header = "Missing") {
                    buildElement {
                        td {
                            +"${it.value.missingWarnings ?: ""}"
                        }
                    }
                }
                column(id = "matched", header = "Matched") {
                    buildElement {
                        td {
                            +"${it.value.matchedWarnings ?: ""}"
                        }
                    }
                }
                column(id = "path", header = "Test file path") { cellProps ->
                    buildElement {
                        td {
                            spread(cellProps.row.getToggleRowExpandedProps())
                            +cellProps.value.filePath
                            attrs.onClickFunction = {
                                GlobalScope.launch {
                                    val te = cellProps.value
                                    val trdi = getDebugInfoFor(te)
                                    if (trdi.ok) {
                                        cellProps.row.original.asDynamic().debugInfo = trdi.decodeFromJsonString<TestResultDebugInfo>()
                                    }
                                    cellProps.row.toggleRowExpanded()
                                }
                            }
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
                column(id = "agentId", header = "Agent ID") {
                    buildElement {
                        td {
                            +"${it.value.agentContainerId}".takeLast(12)
                        }
                    }
                }
            },
            useServerPaging = true,
            usePageSelection = true,
            plugins = arrayOf(
                useFilters,
                useSortBy,
                useExpanded,
                usePagination,
            ),
            renderExpandedRow = { tableInstance, row ->
                // todo: placeholder before, render data once it's available
                val trdi = row.original.asDynamic().debugInfo as TestResultDebugInfo?
                if (trdi != null) {
                    child(testStatusComponent(trdi, tableInstance))
                } else {
                    tr {
                        td {
                            attrs.colSpan = "${tableInstance.columns.size}"
                            +"Debug info not available for this test execution"
                        }
                    }
                }
            },
            additionalOptions = {
                this.asDynamic().manualFilters = true
            },
            commonHeader = { tableInstance ->
                tr {
                    th {
                        attrs.colSpan = "${tableInstance.columns.size}"
                        child(testExecutionFiltersRow(
                            initialValueStatus = state.status?.name ?: ANY,
                            initialValueTestSuite = state.testSuite ?: "",
                            onChangeStatus = { value ->
                                if (value == "ANY") {
                                    setState {
                                        status = null
                                    }
                                } else {
                                    setState {
                                        status = TestResultStatus.valueOf(value)
                                    }
                                }
                            },
                            onChangeTestSuite = { testSuiteValue ->
                                if (testSuiteValue == "") {
                                    setState {
                                        testSuite = null
                                    }
                                } else {
                                    setState {
                                        testSuite = testSuiteValue
                                    }
                                }
                            }))
                    }
                }
            },
            getPageCount = { pageSize ->
                val status = if (state.status != null) "&status=${state.status}" else ""
                val testSuite = if (state.testSuite != null) "&testSuite=${state.testSuite}" else ""
                val count: Int = get(
                    url = "$apiUrl/testExecution/count?executionId=${props.executionId}$status$testSuite",
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
            val status = if (state.status != null) "&status=${state.status}" else ""
            val testSuite = if (state.testSuite != null) "&testSuite=${state.testSuite}" else ""
            get(
                url = "$apiUrl/testExecutions?executionId=${props.executionId}&page=$page&size=$size$status$testSuite",
                headers = Headers().apply {
                    set("Accept", "application/json")
                },
            )
                .unsafeMap {
                    Json.decodeFromString<Array<TestExecutionDto>>(
                        it.text().await()
                    )
                }
                .apply {
                    asDynamic().debugInfo = null
                }
        }) { }
        child(executionTestsNotFound(state.countTests)) {
            attrs.executionDto = state.executionDto
        }
    }
}
