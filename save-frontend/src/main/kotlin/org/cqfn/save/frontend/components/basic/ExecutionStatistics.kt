@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "WildcardImport")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionStatus

import csstype.Width
import react.CSSProperties
import react.Props
import react.dom.*
import react.fc

/**
 * [Props] for execution statistics component
 */
external interface ExecutionStatisticsProps : Props {
    /**
     * And instance of [ExecutionDto], which should be passed from parent component
     */
    var executionDto: ExecutionDto?

    /**
     * Count tests with executionId
     */
    var countTests: Int?
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
    val totalTests = props.countTests?.toLong() ?: 0L
    val isInProgress = props.executionDto?.run { status == ExecutionStatus.RUNNING || status == ExecutionStatus.PENDING } ?: true
    val isSuccess = props.executionDto?.run { passedTests == totalTests } ?: false
    val style = if (isInProgress) {
        "info"
    } else if (isSuccess) {
        "success"
    } else {
        "danger"
    }

    val passedTests = props.executionDto?.passedTests ?: 0L
    val failedTests = props.executionDto?.failedTests ?: 0L
    val runningTests = props.executionDto?.runningTests ?: 0L

    val passRate = props.executionDto?.run {
        if (totalTests > 0) (passedTests.toFloat() / totalTests * 100).toInt() else 0
    } ?: "0"

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
                                        attrs["style"] = kotlinext.js.jso<CSSProperties> {
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
                        div("h5 mb-0 font-weight-bold text-gray-800") { +totalTests.toString() }
                    }
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-info text-uppercase mb-1") { +"Running" }
                        div("h5 mb-0 font-weight-bold text-gray-800") { +runningTests.toString() }
                    }
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-danger text-uppercase mb-1") { +"Failed" }
                        div("h5 mb-0 font-weight-bold text-gray-800") { +failedTests.toString() }
                    }
                    div("col mr-2") {
                        div("text-xs font-weight-bold text-success text-uppercase mb-1") { +"Passed" }
                        div("h5 mb-0 font-weight-bold text-gray-800") { +passedTests.toString() }
                    }
                }
            }
        }
    }
}

/**
 * A component which displays a GIF if tests not found
 *
 * @param count tests for execution
 * @return a functional react component
 */
fun executionTestsNotFound(count: Int?) = fc<ExecutionStatisticsProps> { props ->
    if (count == 0 && props.executionDto?.status != ExecutionStatus.PENDING) {
        div("d-flex justify-content-center") {
            img(src = "img/sad_cat.gif") {}
        }
        div("d-sm-flex align-items-center justify-content-center mb-4 mt-2") {
            h1("h3 mb-0 text-gray-800") {
                +"Tests not found!"
            }
        }
    } else if (count == 0 && props.executionDto?.status == ExecutionStatus.PENDING) {
        div("d-sm-flex align-items-center justify-content-center mb-4 mt-2") {
            h1("h3 mb-0 text-gray-800") {
                +"Execution is starting..."
            }
        }
    }
}
