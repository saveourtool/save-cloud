@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.test.analysis

import com.saveourtool.save.test.analysis.metrics.NoDataAvailable
import com.saveourtool.save.test.analysis.metrics.RegularTestMetrics
import com.saveourtool.save.test.analysis.metrics.TestMetrics
import csstype.ClassName
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul

/**
 * Displays [TestMetrics].
 *
 * @see TestMetrics
 * @see TestMetricsProps
 */
val testMetricsView: FC<TestMetricsProps> = FC { props ->
    div {
        className = ClassName("testMetrics")

        when (val metrics = props.testMetrics) {
            is NoDataAvailable -> noDataAvailable()
            is RegularTestMetrics -> with(metrics) {
                ul {
                    style = listStyle

                    li {
                        nowrap {
                            +"Test runs: "

                            boldText(runCount)

                            +" total / "

                            boldText(failureCount) {
                                color = failureColor
                            }

                            +" failure(s) / "

                            boldText(ignoredCount)

                            +" ignored"
                        }
                    }

                    li {
                        labelledValue {
                            label = "Failure rate"
                            value = "$failureRatePercentage%"
                        }
                    }

                    li {
                        labelledValue {
                            label = "Flip rate"
                            labelTooltip = FLIP_RATE_DESCRIPTION
                            value = "$flipRatePercentage%"
                        }
                    }

                    averageDurationOrNull?.let { averageDuration ->
                        li {
                            labelledValue {
                                label = "Duration (average)"
                                value = averageDuration
                            }
                        }
                    }

                    medianDurationOrNull?.let { medianDuration ->
                        li {
                            labelledValue {
                                label = "Duration (median)"
                                value = medianDuration
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Properties of [testMetricsView].
 *
 * @see testMetricsView
 */
external interface TestMetricsProps : Props {
    /**
     * The test metrics to render.
     */
    var testMetrics: TestMetrics
}
