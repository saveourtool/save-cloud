@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.agent.TestSuiteExecutionStatisticDto
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.externals.chart.DataPieChart
import org.cqfn.save.frontend.externals.chart.pieChart
import org.cqfn.save.frontend.externals.chart.randomColor
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.unsafeMap

import org.w3c.fetch.Headers
import react.*
import react.dom.div
import react.dom.td
import react.table.columns

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
 * @param onOpenMenuStatistic
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
fun projectStatisticMenu(
    onOpenMenuStatistic: (isOpen: Boolean) -> Unit,
) =
        fc<ProjectStatisticMenuProps> { props ->

            val (latestExecutionStatisticDtos, setLatestExecutionStatisticDtos) = useState(props.latestExecutionStatisticDtos)
            val scope = CoroutineScope(Dispatchers.Default)

            if (props.isOpen == true) {
                useEffect(listOf<dynamic>(props.executionId, props.latestExecutionStatisticDtos)) {
                    scope.launch {
                        val testLatestExecutions = get(
                            url = "$apiUrl/testLatestExecutions?executionId=${props.executionId}&status=${TestResultStatus.PASSED}",
                            headers = Headers().also {
                                it.set("Accept", "application/json")
                            },
                        )
                            .unsafeMap {
                                it.decodeFromJsonString<List<TestSuiteExecutionStatisticDto>>()
                            }
                        setLatestExecutionStatisticDtos(testLatestExecutions)
                    }
                    onOpenMenuStatistic(false)
                }
            } else {
                useEffect(listOf<dynamic>()) {
                    cleanup {
                        if (scope.isActive) {
                            scope.cancel()
                        }
                    }
                }
            }

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

                    child(tableComponent(
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
                    ) { page, size ->
                        get(
                            url = "$apiUrl/testLatestExecutions?executionId=${props.executionId}&status=${TestResultStatus.PASSED}&page=$page&size=$size",
                            headers = Headers().also {
                                it.set("Accept", "application/json")
                            },
                        )
                            .unsafeMap {
                                it.decodeFromJsonString<Array<TestSuiteExecutionStatisticDto>>()
                            }
                    }) { }
                }
            }
        }
