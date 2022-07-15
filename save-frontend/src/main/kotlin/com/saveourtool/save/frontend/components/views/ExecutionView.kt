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

import csstype.*
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.tr
import react.table.*
import react.table.columns

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.js.jso
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * [Props] for execution results view
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
     * Test Result Status to filter by
     */
    var testName: String?

    /**
     * Name of test suite
     */
    var testSuite: String?

    /**
     * Test Result Status to filter by
     */
    var tag: String?
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
     * Test Result Status to filter by
     */
    var testName: String?

    /**
     * Name of test suite
     */
    var testSuite: String?

    /**
     * Test Result Status to filter by
     */
    var tag: String?
}

/**
 * A Component for execution view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("MAGIC_NUMBER", "GENERIC_VARIABLE_WRONG_DECLARATION")
class ExecutionView : AbstractView<ExecutionProps, ExecutionState>(false) {
    private val testExecutionFiltersRow = testExecutionFiltersRow(
        initialValueStatus = state.status?.name ?: ANY,
        initialValueTestName = state.testName ?: "",
        initialValueTestSuite = state.testSuite ?: "",
        initialValueTag = state.tag ?: "",
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
        onChangeTestName = { testNameValue ->
            if (testNameValue == "") {
                setState {
                    testName = null
                }
            } else {
                setState {
                    testName = testNameValue
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
        },
        onChangeTag = { tagValue ->
            if (tagValue == "") {
                setState {
                    tag = null
                }
            } else {
                setState {
                    tag = tagValue
                }
            }
        }
    )
    private val testExecutionsTable = tableComponent<TestExecutionDto, StatusProps<TestExecutionDto>>(
        columns = columns {
            column(id = "index", header = "#") {
                Fragment.create {
                    td {
                        +"${it.row.index + 1 + it.state.pageIndex * it.state.pageSize}"
                    }
                }
            }
            column(id = "startTime", header = "Start time", { startTimeSeconds }) { cellProps ->
                Fragment.create {
                    td {
                        +"${
                            cellProps.value?.let { Instant.fromEpochSeconds(it, 0) }
                            ?: "Running"
                        }"
                    }
                }
            }
            column(id = "endTime", header = "End time", { endTimeSeconds }) { cellProps ->
                Fragment.create {
                    td {
                        +"${
                            cellProps.value?.let { Instant.fromEpochSeconds(it, 0) }
                            ?: "Running"
                        }"
                    }
                }
            }
            column(id = "status", header = "Status", { status.name }) {
                Fragment.create {
                    td {
                        +it.value
                    }
                }
            }
            column(id = "missing", header = "Missing", { unmatched }) {
                Fragment.create {
                    td {
                        +"${it.value ?: ""}"
                    }
                }
            }
            column(id = "matched", header = "Matched", { matched }) {
                Fragment.create {
                    td {
                        +"${it.value ?: ""}"
                    }
                }
            }
            column(id = "path", header = "File Name") { cellProps ->
                Fragment.create {
                    td {
                        spread(cellProps.row.getToggleRowExpandedProps())

                        val testName = cellProps.value.filePath
                        val shortTestName = if (testName.length > 35) "${testName.take(15)} ... ${testName.takeLast(15)}" else testName
                        +shortTestName

                        // debug info is provided by agent after the execution
                        // possibly there can be cases when this info is not available
                        if (cellProps.value.hasDebugInfo == true) {
                            style = jso {
                                textDecoration = "underline".unsafeCast<TextDecoration>()
                                color = "blue".unsafeCast<Color>()
                                cursor = "pointer".unsafeCast<Cursor>()
                            }

                            onClick = {
                                this@ExecutionView.scope.launch {
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
                Fragment.create {
                    td {
                        +it.value
                    }
                }
            }
            column(id = "suiteName", header = "Test suite", { testSuiteName }) {
                Fragment.create {
                    td {
                        +"${it.value}"
                    }
                }
            }
            column(id = "tags", header = "Tags") {
                Fragment.create {
                    td {
                        +"${it.value.tags}"
                    }
                }
            }
            column(id = "agentId", header = "Agent ID") {
                Fragment.create {
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
                        + "No info available yet for this test execution"
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
                    testExecutionFiltersRow()
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
            arrayOf(it.status, it.testName, it.testSuite, it.tag)
        }
    )

    init {
        state.executionDto = null
        state.status = null
        state.testName = null
        state.testSuite = null
        state.tag = null
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
    override fun ChildrenBuilder.render() {
        div {
            div {
                className = ClassName("d-flex")
                val statusVal = state.executionDto?.status
                val statusColor = when (statusVal) {
                    ExecutionStatus.ERROR -> "bg-danger"
                    ExecutionStatus.RUNNING, ExecutionStatus.PENDING -> "bg-info"
                    ExecutionStatus.FINISHED -> "bg-success"
                    else -> "bg-secondary"
                }

                div {
                    className = ClassName("col-md-2 mb-4")
                    div {
                        className = ClassName("card $statusColor text-white h-100 shadow py-2")
                        div {
                            className = ClassName("card-body")
                            +(statusVal?.name ?: "N/A")
                            div {
                                className = ClassName("text-white-50 small")
                                +"Project version: ${(state.executionDto?.version ?: "N/A")}"
                            }
                        }
                    }
                }

                executionStatistics {
                    executionDto = state.executionDto
                }

                div {
                    className = ClassName("col-md-3 mb-4")
                    div {
                        className = ClassName("card border-left-info shadow h-100 py-2")
                        div {
                            className = ClassName("card-body")
                            div {
                                className = ClassName("row no-gutters align-items-center mx-auto")
                                a {
                                    href = ""
                                    +"Rerun execution"
                                    fontAwesomeIcon(icon = faRedo, classes = "ml-2")
                                    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
                                    onClick = { event ->
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
        testExecutionsTable {
            status = state.status
            testName = state.testName
            testSuite = state.testSuite
            tag = state.tag
            getData = { page, size ->
                val paramString = setStatusAndNameAndSuiteAndTag()
                get(
                    url = "$apiUrl/testExecutions?executionId=${props.executionId}&page=$page&size=$size$paramString&checkDebugInfo=true",
                    headers = Headers().apply {
                        set("Accept", "application/json")
                    },
                    loadingHandler = ::classLoadingHandler,
                ).unsafeMap {
                    Json.decodeFromString<Array<TestExecutionDto>>(
                        it.text().await()
                    )
                }.apply {
                        asDynamic().debugInfo = null
                }
            }
            getPageCount = { pageSize ->
                val paramString = setStatusAndNameAndSuiteAndTag()
                val count: Int = get(
                    url = "$apiUrl/testExecution/count?executionId=${props.executionId}$paramString",
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
        executionTestsNotFound {
            executionDto = state.executionDto
        }
    }

    private fun setStatusAndNameAndSuiteAndTag(): String?{
        val status1 = state.status?.let {
            "&status=${state.status}"
        } ?: run {
            ""
        }
        val testName1 = state.testName?.let {
            "&testName=${state.testName}"
        } ?: run {
            ""
        }
        val testSuite1 = state.testSuite?.let {
            "&testSuite=${state.testSuite}"
        } ?: run {
            ""
        }
        val tag1 = state.tag?.let {
            "&tag=${state.tag}"
        } ?: run {
            ""
        }
        return status1 + testName1 + testSuite1 + tag1
    }

    companion object : RStatics<ExecutionProps, ExecutionState, ExecutionView, Context<RequestStatusContext>>(ExecutionView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
