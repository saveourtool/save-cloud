/**
 * View for displaying individual execution results
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.result.CountWarnings
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.filters.TestExecutionFilters
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.externals.table.useFilters
import com.saveourtool.save.frontend.http.getDebugInfoFor
import com.saveourtool.save.frontend.http.getExecutionInfoFor
import com.saveourtool.save.frontend.themes.Colors
import com.saveourtool.save.frontend.utils.*

import csstype.*
import org.w3c.fetch.Headers
import react.*
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
     * All filters in one value [filters]
     */
    var filters: TestExecutionFilters
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
        columns = {
            columns {
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
                            +formatCounter(it.value)
                        }
                    }
                }
                column(id = "matched", header = "Matched", { matched }) {
                    Fragment.create {
                        td {
                            +formatCounter(it.value)
                        }
                    }
                }
                column(id = "path", header = "Test Name") { cellProps ->
                    Fragment.create {
                        td {
                            val testName = cellProps.value.filePath
                            val shortTestName =
                                    if (testName.length > 35) "${testName.take(15)} ... ${testName.takeLast(15)}" else testName
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
                trei?.failReason != null || trdi != null -> {
                    trei?.failReason?.let { executionStatusComponent(it, tableInstance)() }
                    trdi?.let { testStatusComponent(it, tableInstance)() }
                }
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
                            window.location.href = getUrlWithFiltersParams(filterValue)
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

    private fun formatCounter(count: Long?): String = count?.let {
        if (CountWarnings.isNotApplicable(it.toInt())) {
            "N/A"
        } else {
            it.toString()
        }
    } ?: ""

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
                filters = props.filters
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
                displayExecutionInfoHeader(state.executionDto, false, "row mb-2") { event ->
                    scope.launch {
                        val response = post(
                            "$apiUrl/run/re-trigger?executionId=${props.executionId}",
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

        // fixme: table is rendered twice because of state change when `executionDto` is fetched
        testExecutionsTable {
            filters = state.filters
            getData = { page, size ->
                post(
                    url = "$apiUrl/test-executions?executionId=${props.executionId}&page=$page&size=$size&checkDebugInfo=true",
                    headers = jsonHeaders,
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
        displayTestNotFound(state.executionDto)
    }

    private fun getUrlWithFiltersParams(filterValue: TestExecutionFilters) = "${window.location.href.substringBefore("?")}${filterValue.toQueryParams()}"

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
