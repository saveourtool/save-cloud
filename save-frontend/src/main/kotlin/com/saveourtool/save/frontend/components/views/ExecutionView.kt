/**
 * View for displaying individual execution results
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.basic.SelectOption.Companion.ANY
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.externals.fontawesome.faRedo
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.externals.table.useFilters
import com.saveourtool.save.frontend.http.getDebugInfoFor
import com.saveourtool.save.frontend.http.getExecutionInfoFor
import com.saveourtool.save.frontend.themes.Colors
import com.saveourtool.save.frontend.utils.*

import csstype.Background
import csstype.Color
import csstype.Cursor
import csstype.TextDecoration
import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import react.table.*
import react.table.columns

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.html.js.onClickFunction
import kotlinx.js.jso
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
     * Test Result Status to filter by
     */
    var status: TestResultStatus?

    /**
     * Name of test suite
     */
    var testSuite: String?
}

/**
 * [Props] of a data table with status and testSuite
 */
external interface StatusProps<D : Any> : TableProps<D> {
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
@Suppress("MAGIC_NUMBER", "GENERIC_VARIABLE_WRONG_DECLARATION")
class ExecutionView : AbstractView<ExecutionProps, ExecutionState>(false) {
    private val testExecutionFiltersRow = testExecutionFiltersRow(
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
        }
    )
    private val testExecutionsTable = tableComponent<TestExecutionDto, StatusProps<TestExecutionDto>>(
        columns = columns {
            column(id = "index", header = "#") {
                buildElement {
                    td {
                        +"${it.row.index + 1 + it.state.pageIndex * it.state.pageSize}"
                    }
                }
            }
            column(id = "startTime", header = "Start time", { startTimeSeconds }) { cellProps ->
                buildElement {
                    td {
                        +"${
                            cellProps.value?.let { Instant.fromEpochSeconds(it, 0) }
                            ?: "Running"
                        }"
                    }
                }
            }
            column(id = "endTime", header = "End time", { endTimeSeconds }) { cellProps ->
                buildElement {
                    td {
                        +"${
                            cellProps.value?.let { Instant.fromEpochSeconds(it, 0) }
                            ?: "Running"
                        }"
                    }
                }
            }
            column(id = "status", header = "Status", { status.name }) {
                buildElement {
                    td {
                        +it.value
                    }
                }
            }
            column(id = "missing", header = "Missing", { unmatched }) {
                buildElement {
                    td {
                        +"${it.value ?: ""}"
                    }
                }
            }
            column(id = "matched", header = "Matched", { matched }) {
                buildElement {
                    td {
                        +"${it.value ?: ""}"
                    }
                }
            }
            column(id = "path", header = "Test file path") { cellProps ->
                buildElement {
                    td {
                        spread(cellProps.row.getToggleRowExpandedProps())

                        val testName = cellProps.value.filePath
                        val shortTestName = if (testName.length > 35) "${testName.take(15)} ... ${testName.takeLast(15)}" else testName
                        +shortTestName

                        // debug info is provided by agent after the execution
                        // possibly there can be cases when this info is not available
                        if (cellProps.value.hasDebugInfo == true) {
                            attrs["style"] = jso<CSSProperties> {
                                textDecoration = "underline".unsafeCast<TextDecoration>()
                                color = "blue".unsafeCast<Color>()
                                cursor = "pointer".unsafeCast<Cursor>()
                            }

                            attrs.onClickFunction = {
                                scope.launch {
                                    val testExecution = cellProps.value
                                    val trDebugInfoRequest = getDebugInfoFor(testExecution)
                                    if (trDebugInfoRequest.ok) {
                                        cellProps.row.original.asDynamic().debugInfo =
                                                trDebugInfoRequest.decodeFromJsonString<TestResultDebugInfo>()
                                    }
                                    val trExecutionInfo = getExecutionInfoFor(testExecution)
                                    if (trExecutionInfo.ok) {
                                        cellProps.row.original.asDynamic().executionInfo =
                                                trExecutionInfo.decodeFromJsonString<ExecutionUpdateDto>()
                                    }
                                    cellProps.row.toggleRowExpanded()
                                }
                            }
                        }
                    }
                }
            }
            column(id = "plugin", header = "Plugin type", { pluginName }) {
                buildElement {
                    td {
                        +it.value
                    }
                }
            }
            column(id = "suiteName", header = "Test suite", { testSuiteName }) {
                buildElement {
                    td {
                        +"${it.value}"
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
            val trei = row.original.asDynamic().executionInfo as ExecutionUpdateDto?
            trei?.failReason?.let {
                executionStatusComponent(it, tableInstance)
            }
            val trdi = row.original.asDynamic().debugInfo as TestResultDebugInfo?
            trdi?.let {
                testStatusComponent(trdi, tableInstance)
            } ?: trei ?: run {
                tr {
                    td {
                        colSpan = tableInstance.columns.size
                        +"No info available yet for this test execution"
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
                    colSpan = tableInstance.columns.size
                    testExecutionFiltersRow
                }
            }
        },
        getRowProps = { row ->
            val color = when (row.original.status) {
                TestResultStatus.FAILED -> Colors.RED
                TestResultStatus.IGNORED -> Colors.GOLD
                TestResultStatus.READY_FOR_TESTING, TestResultStatus.RUNNING -> Colors.GREY
                TestResultStatus.INTERNAL_ERROR, TestResultStatus.TEST_ERROR -> Colors.DARK_RED
                TestResultStatus.PASSED -> Colors.GREEN
            }
            jso {
                style = jso {
                    background = color.value.unsafeCast<Background>()
                }
            }
        },
        getAdditionalDependencies = {
            arrayOf(it.status, it.testSuite)
        }
    )

    init {
        state.executionDto = null
        state.status = null
        state.testSuite = null
    }

    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            val headers = Headers().also { it.set("Accept", "application/json") }
            val executionDtoFromBackend: ExecutionDto =
                    get(
                        "$apiUrl/executionDto?executionId=${props.executionId}",
                        headers,
                        loadingHandler = ::classLoadingHandler,
                    )
                        .decodeFromJsonString()
            setState {
                executionDto = executionDtoFromBackend
                status = props.status
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
            div("d-flex") {
                val statusVal = state.executionDto?.status
                val statusColor = when (statusVal) {
                    ExecutionStatus.ERROR -> "bg-danger"
                    ExecutionStatus.RUNNING, ExecutionStatus.PENDING -> "bg-info"
                    ExecutionStatus.FINISHED -> "bg-success"
                    else -> "bg-secondary"
                }

                div("col-md-2 mb-4") {
                    div("card $statusColor text-white h-100 shadow py-2") {
                        div("card-body") {
                            +(statusVal?.name ?: "N/A")
                            div("text-white-50 small") { +"Project version: ${(state.executionDto?.version ?: "N/A")}" }
                        }
                    }
                }

                child(executionStatistics) {
                    attrs.executionDto = state.executionDto
                }

                div("col-md-3 mb-4") {
                    div("card border-left-info shadow h-100 py-2") {
                        div("card-body") {
                            div("row no-gutters align-items-center mx-auto") {
                                a("") {
                                    +"Rerun execution"
                                    fontAwesomeIcon(icon = faRedo, classes = "ml-2")
                                    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
                                    attrs.onClickFunction = { event ->
                                        scope.launch {
                                            val response = post(
                                                "$apiUrl/rerunExecution?id=${props.executionId}",
                                                Headers(),
                                                undefined,
                                                loadingHandler = ::classLoadingHandler,
                                            )
                                            if (response.ok) {
                                                window.alert("Rerun request successfully submitted")
                                                window.location.reload()
                                            }
                                        }
                                        event.preventDefault()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // fixme: table is rendered twice because of state change when `executionDto` is fetched
        child(testExecutionsTable) {
            attrs.status = state.status
            attrs.testSuite = state.testSuite
            attrs.getData = { page, size ->
                val status = state.status?.let {
                    "&status=${state.status}"
                }
                    ?: run {
                        ""
                    }
                val testSuite = state.testSuite?.let {
                    "&testSuite=${state.testSuite}"
                }
                    ?: run {
                        ""
                    }
                get(
                    url = "$apiUrl/testExecutions?executionId=${props.executionId}&page=$page&size=$size$status$testSuite&checkDebugInfo=true",
                    headers = Headers().apply {
                        set("Accept", "application/json")
                    },
                    loadingHandler = ::classLoadingHandler,
                ).unsafeMap {
                    Json.decodeFromString<Array<TestExecutionDto>>(
                        it.text().await()
                    )
                }
                    .apply {
                        asDynamic().debugInfo = null
                    }
            }
            attrs.getPageCount = { pageSize ->
                val status = state.status?.let {
                    "&status=${state.status}"
                }
                    ?: run {
                        ""
                    }
                val testSuite = state.testSuite?.let {
                    "&testSuite=${state.testSuite}"
                }
                    ?: run {
                        ""
                    }
                val count: Int = get(
                    url = "$apiUrl/testExecution/count?executionId=${props.executionId}$status$testSuite",
                    headers = Headers().also {
                        it.set("Accept", "application/json")
                    },
                    loadingHandler = ::classLoadingHandler,
                )
                    .json()
                    .await()
                    .unsafeCast<Int>()
                count / pageSize + 1
            }
        }
        child(executionTestsNotFound) {
            attrs.executionDto = state.executionDto
        }
    }

    companion object : RStatics<ExecutionProps, ExecutionState, ExecutionView, Context<RequestStatusContext>>(ExecutionView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
