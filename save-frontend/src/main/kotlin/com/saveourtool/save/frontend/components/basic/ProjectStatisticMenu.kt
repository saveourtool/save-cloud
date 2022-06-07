@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.agent.TestSuiteExecutionStatisticDto
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.externals.chart.DataPieChart
import com.saveourtool.save.frontend.externals.chart.pieChart
import com.saveourtool.save.frontend.externals.chart.randomColor
import com.saveourtool.save.frontend.utils.*
import org.w3c.fetch.Headers
import react.*
import react.dom.div
import react.dom.h6
import react.dom.td
import react.table.columns

@Suppress("MAGIC_NUMBER")
private val executionDetailsTable = tableComponent(
        columns = columns<TestSuiteExecutionStatisticDto> {
            column(id = "name", header = "Test suite", { testSuiteName }) {
                buildElement {
                    td {
                        +it.value
                    }
                }
            }
            column(id = "tests", header = "Number of tests", { countTest }) {
                buildElement {
                    td {
                        +"${it.value}"
                    }
                }
            }
            column(id = "rate", header = "Passed tests", { countWithStatusTest }) {
                buildElement {
                    td {
                        +"${it.value}"
                    }
                }
            }
        },
        initialPageSize = 10,
        useServerPaging = false,
        usePageSelection = false,
    )


/**
 * ProjectStatisticMenu component props
 */
external interface ProjectStatisticMenuProps : Props {
    /**
     * Id of execution
     */
    var executionId: Long?

    /**
     * list of tests
     */
    var latestExecutionStatisticDtos: List<TestSuiteExecutionStatisticDto>?

    /**
     * Flag to open Menu
     */
    var isOpen: Boolean?
}

/**
 * @return ReactElement
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "AVOID_NULL_CHECKS"
)
fun projectStatisticMenu(
) = fc<ProjectStatisticMenuProps> { props ->
    val (latestExecutionStatisticDtos, setLatestExecutionStatisticDtos) = useState(props.latestExecutionStatisticDtos)

    useRequest(arrayOf(props.executionId, props.latestExecutionStatisticDtos, props.isOpen), isDeferred = false) {
        if (props.isOpen != true && props.executionId != null) {
            val testLatestExecutions = get(
                url = "$apiUrl/testLatestExecutions?executionId=${props.executionId}&status=${TestResultStatus.PASSED}",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                },
                loadingHandler = ::loadingHandler,
            )
                .unsafeMap {
                    it.decodeFromJsonString<List<TestSuiteExecutionStatisticDto>>()
                }
            setLatestExecutionStatisticDtos(testLatestExecutions)
        }
    }()

    div("row justify-content-center") {
        // ===================== LEFT COLUMN =======================================================================
        div("col-2 mr-3") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Total number of tests by test suite"
            }
            div("col-xl col-md-6 mb-4") {
                val data = latestExecutionStatisticDtos?.map {
                    DataPieChart(it.testSuiteName, it.countTest, randomColor())
                } ?: emptyList()
                pieChart(
                    data.toTypedArray()
                ) {
                    attrs.animate = true
                    attrs.segmentsShift = 2
                    attrs.radius = 47
                }
            }
        }
        // ===================== RIGHT COLUMN =======================================================================
        div("col-6") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Latest execution"
            }

            if (props.executionId != null && latestExecutionStatisticDtos?.isNotEmpty() == true) {
                executionDetailsTable {
                    attrs.getData = { page, size ->
                        get(
                            url = "$apiUrl/testLatestExecutions?executionId=${props.executionId}&status=${TestResultStatus.PASSED}&page=$page&size=$size",
                            headers = Headers().also {
                                it.set("Accept", "application/json")
                            },
                            loadingHandler = ::loadingHandler,
                        )
                            .unsafeMap {
                                it.decodeFromJsonString<Array<TestSuiteExecutionStatisticDto>>()
                            }
                    }
                }
            } else {
                div("card shadow mb-4") {
                    div("card-header py-3") {
                        h6("m-0 font-weight-bold text-primary text-center") {
                            +"No executions yet"
                        }
                    }
                }
            }
        }
    }
}
