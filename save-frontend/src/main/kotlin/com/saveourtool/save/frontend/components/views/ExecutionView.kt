/**
 * View for displaying individual execution results
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.execution.TestExecutionFilters
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.*
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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
     * All filters in one value [filters]
     */
    var filters: TestExecutionFilters
}

/**
 * [Props] of a data table with status and testSuite
 */
external interface StatusProps<D : Any> : TableProps<D> {
    /**
     * All filters in one value [filters]
     */
    var filters: TestExecutionFilters
}

/**
 * A Component for execution view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("MAGIC_NUMBER", "GENERIC_VARIABLE_WRONG_DECLARATION")
class ExecutionView : AbstractView<ExecutionProps, ExecutionState>(false) {
    @Suppress("TYPE_ALIAS")
    private val additionalInfo: MutableMap<IdType<*>, AdditionalRowInfo> = mutableMapOf()
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
                        val testName = cellProps.value.filePath
                        val shortTestName = if (testName.length > 35) "${testName.take(15)} ... ${testName.takeLast(15)}" else testName
                        +shortTestName

                        // debug info is provided by agent after the execution
                        // possibly there can be cases when this info is not available
                        if (cellProps.value.hasDebugInfo == true) {
                            spread(cellProps.row.getToggleRowExpandedProps())
                            style = jso {
                                textDecoration = "underline".unsafeCast<TextDecoration>()
                                color = "blue".unsafeCast<Color>()
                                cursor = "pointer".unsafeCast<Cursor>()
                            }

                            onClick = {
                                this@ExecutionView.scope.launch {
                                    if (!cellProps.row.isExpanded) {
                                        getAdditionalInfoFor(cellProps.value, cellProps.row.id)
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
            val (errorDescription, trdi, trei) = additionalInfo[row.id] ?: AdditionalRowInfo()
            when {
                errorDescription != null -> tr {
                    td {
                        colSpan = tableInstance.columns.size
                        +"Error retrieving additional information: $errorDescription"
                    }
                }
                trei?.failReason != null -> executionStatusComponent(trei.failReason!!, tableInstance)()
                trdi != null -> testStatusComponent(trdi, tableInstance)()
                else -> tr {
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
                    testExecutionFiltersRow {
                        filters = state.filters
                        onChangeFilters = { filterValue ->
                            if (filterValue.status == null || filterValue.status?.name == "ANY") {
                                setState {
                                    filters = filters.copy(status = null)
                                }
                            } else {
                                setState {
                                    filters = filters.copy(status = filterValue.status)
                                }
                            }
                            if (filterValue.fileName?.isEmpty() == true) {
                                setState {
                                    filters = filters.copy(fileName = null)
                                }
                            } else {
                                setState {
                                    filters = filters.copy(fileName = filterValue.fileName)
                                }
                            }
                            if (filterValue.testSuite?.isEmpty() == true) {
                                setState {
                                    filters = filters.copy(testSuite = null)
                                }
                            } else {
                                setState {
                                    filters = filters.copy(testSuite = filterValue.testSuite)
                                }
                            }
                            if (filterValue.tag?.isEmpty() == true) {
                                setState {
                                    filters = filters.copy(tag = null)
                                }
                            } else {
                                setState {
                                    filters = filters.copy(tag = filterValue.tag)
                                }
                            }
                        }
                    }
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
            arrayOf(it.filters)
        }
    )

    init {
        state.executionDto = null
        state.filters = TestExecutionFilters.empty
    }

    private suspend fun getAdditionalInfoFor(testExecution: TestExecutionDto, id: IdType<*>) {
        val trDebugInfoResponse = getDebugInfoFor(testExecution)
        val trExecutionInfoResponse = getExecutionInfoFor(testExecution)
        // there may be errors during deserialization, which will otherwise be silently ignored
        try {
            additionalInfo[id] = AdditionalRowInfo()
            if (trDebugInfoResponse.ok) {
                additionalInfo[id] = additionalInfo[id]!!
                    .copy(testResultDebugInfo = trDebugInfoResponse.decodeFromJsonString<TestResultDebugInfo>())
            }
            if (trExecutionInfoResponse.ok) {
                additionalInfo[id] = additionalInfo[id]!!
                    .copy(executionInfo = trExecutionInfoResponse.decodeFromJsonString<ExecutionUpdateDto>())
            }
        } catch (ex: SerializationException) {
            additionalInfo[id] = additionalInfo[id]!!
                .copy(errorDescription = ex.describe())
        }
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
                filters = filters.copy(status = props.status)
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
                                                body = undefined,
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
            filters = state.filters
            getData = { page, size ->
                post(
                    url = "$apiUrl/test-executions?executionId=${props.executionId}&page=$page&size=$size&checkDebugInfo=true",
                    headers = Headers().apply {
                        set("Accept", "application/json")
                        set("Content-Type", "application/json")
                    },
                    body = Json.encodeToString(filters),
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
                val filtersQueryString = buildString {
                    filters.status?.let {
                        append("&status=${filters.status}")
                    } ?: append("")

                    filters.testSuite?.let {
                        append("&testSuite=${filters.testSuite}")
                    } ?: append("")
                }

                val count: Int = get(
                    url = "$apiUrl/testExecution/count?executionId=${props.executionId}$filtersQueryString",
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

    companion object : RStatics<ExecutionProps, ExecutionState, ExecutionView, Context<RequestStatusContext>>(ExecutionView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}

/**
 * @property errorDescription if retrieved data can't be parsed, this field should contain description of the error
 * @property testResultDebugInfo
 * @property executionInfo
 */
private data class AdditionalRowInfo(
    val errorDescription: String? = null,
    val testResultDebugInfo: TestResultDebugInfo? = null,
    val executionInfo: ExecutionUpdateDto? = null,
)
