@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.agent.LatestExecutionStatisticDto
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.externals.chart.DataPieChart
import org.cqfn.save.frontend.externals.chart.pieChart
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.unsafeMap

import org.w3c.fetch.Headers
import react.*
import react.dom.div
import react.dom.td
import react.table.columns

/**
 * ProjectStatisticMenu component props
 */
external interface ProjectStatisticMenuProps : Props {
    /**
     * Id of execution
     */
    var executionId: Long?
}

/**
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "MAGIC_NUMBER")
fun projectStatisticMenu() =
        fc<ProjectStatisticMenuProps> { props ->

            div("row justify-content-center") {
                // ===================== LEFT COLUMN =======================================================================
                div("col-2 mr-3") {
                    div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                        +"Chart pie"
                    }

                    div("col-xl col-md-6 mb-4") {
                        val data = arrayOf(
                            DataPieChart("One", 10, "#E38627"),
                            DataPieChart("Two", 15, "#C13C37"),
                            DataPieChart("Three", 20, "#6A2135"),
                        )

                        pieChart(
                            data
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
                        columns = columns<LatestExecutionStatisticDto> {
                            column(id = "name", header = "Test suite", { testSuiteName }) {
                                buildElement {
                                    td {
                                        +it.value
                                    }
                                }
                            }
                            column(id = "tests", header = "Number of test", { countTest }) {
                                buildElement {
                                    td {
                                        +"${it.value}"
                                    }
                                }
                            }
                            column(id = "rate", header = "Passed test", { countPassedTest }) {
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
                    ) { _, _ ->
                        get(
                            url = "$apiUrl/testLatestExecutions?executionId=${props.executionId}&status=${TestResultStatus.PASSED}",
                            headers = Headers().also {
                                it.set("Accept", "application/json")
                            },
                        )
                            .unsafeMap {
                                it.decodeFromJsonString<Array<LatestExecutionStatisticDto>>()
                            }
                    }) { }
                }
            }
        }
