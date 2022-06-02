@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.ExecutionStatus

import csstype.Width
import react.CSSProperties
import react.Props
import react.dom.*
import react.fc

import kotlinx.js.jso

/**
 * [Props] for execution statistics component
 */
external interface ExecutionStatisticsProps : Props {
    /**
     * And instance of [ExecutionDto], which should be passed from parent component
     */
    var executionDto: ExecutionDto?
}

/**
 * A component which displays statistics about an execution from its props
 *
 * @param classes HTML classes for the enclosing div
 * @return a functional react component
 */
@Suppress(
    "MAGIC_NUMBER",
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "ComplexMethod"
)
fun executionStatistics(classes: String = "") = fc<ExecutionStatisticsProps> { props ->
    val isInProgress = props.executionDto?.run { status == ExecutionStatus.RUNNING || status == ExecutionStatus.PENDING } ?: true
    val isSuccess = props.executionDto?.run { passedTests == allTests } ?: false
    val style = if (isInProgress) {
        "info"
    } else if (isSuccess) {
        "success"
    } else {
        "danger"
    }

    val allTests = props.executionDto?.allTests?.toString() ?: "0"
    val passedTests = props.executionDto?.passedTests?.toString() ?: "0"
    val failedTests = props.executionDto?.failedTests?.toString() ?: "0"
    val runningTests = props.executionDto?.runningTests?.toString() ?: "0"

    val passRate = props.executionDto
        ?.let { calculateRate(it.passedTests, it.allTests) }
        ?: "0"
    val precisionRate = props.executionDto
        ?.let { calculateRate(it.matchedChecks, it.matchedChecks + it.unmatchedChecks) }
        ?: "0"
    val recallRate = props.executionDto
        ?.let { calculateRate(it.matchedChecks, it.expectedChecks) }
        ?: "0"

    div("col-xl-3 col-md-6 mb-4") {
        div("card border-left-info shadow h-100 py-2") {
            div("card-body") {
                div("row no-gutters align-items-center") {
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-info text-uppercase mb-1") { +"Pass Rate" }
                        div("row no-gutters align-items-center") {
                            div("col-auto") {
                                div("h5 mb-0 mr-3 font-weight-bold text-gray-800") { +"$passRate%" }
                            }
                            div("col") {
                                div("progress progress-sm mr-2") {
                                    div("progress-bar bg-info") {
                                        attrs["role"] = "progressbar"
                                        attrs["style"] = jso<CSSProperties> {
                                            width = "$passRate%".unsafeCast<Width>()
                                        }
                                        attrs["aria-valuenow"] = passRate
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
        div("card border-left-$style shadow h-100 py-2") {
            div("card-body") {
                div("row no-gutters align-items-center") {
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-info text-uppercase mb-1") { +"Tests" }
                        div("h5 mb-0 font-weight-bold text-gray-800") { +allTests }
                    }
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-info text-uppercase mb-1") { +"Running" }
                        div("h5 mb-0 font-weight-bold text-gray-800") { +runningTests }
                    }
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-danger text-uppercase mb-1") { +"Failed" }
                        div("h5 mb-0 font-weight-bold text-gray-800") { +failedTests }
                    }
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-success text-uppercase mb-1") { +"Passed" }
                        div("h5 mb-0 font-weight-bold text-gray-800") { +passedTests }
                    }
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-info text-uppercase mb-1") { +"Precision" }
                        div("h5 mb-0 font-weight-bold text-gray-800") { +precisionRate }
                    }
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-info text-uppercase mb-1") { +"Recall" }
                        div("h5 mb-0 font-weight-bold text-gray-800") { +recallRate }
                    }
                }
            }
        }
    }
}

private fun calculateRate(numerator: Long, denominator: Long) = denominator.takeIf { it > 0 }
    ?.run { numerator.toDouble() / denominator }
    ?.let { it * 100 }
    ?.toInt()
    ?.toString()

/**
 * A component which displays a GIF if tests not found
 *
 * @return a functional react component
 */
fun executionTestsNotFound() = fc<ExecutionStatisticsProps> { props ->
    val allTests = props.executionDto?.allTests
    val status = props.executionDto?.status
    if (allTests == 0L && status != ExecutionStatus.PENDING) {
        div("d-flex justify-content-center") {
            img(src = "img/sad_cat.gif") {}
        }
        div("d-sm-flex align-items-center justify-content-center mb-4 mt-2") {
            h1("h3 mb-0 text-gray-800") {
                +"Tests not found!"
            }
        }
    } else if (allTests == 0L && status == ExecutionStatus.PENDING) {
        div("d-sm-flex align-items-center justify-content-center mb-4 mt-2") {
            h1("h3 mb-0 text-gray-800") {
                +"Execution is starting..."
            }
        }
    }
}
