@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.core.result.Crash
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
import com.saveourtool.save.frontend.http.getDebugInfoFor
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.multilineText
import com.saveourtool.save.frontend.utils.multilineTextWithIndices

import org.w3c.fetch.Headers
import react.Props
import react.RBuilder
import react.dom.br
import react.dom.div
import react.dom.samp
import react.dom.small
import react.dom.table
import react.dom.tbody
import react.dom.td
import react.dom.tr
import react.fc
import react.router.useParams
import react.useState

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Suppress("TOO_LONG_FUNCTION", "EMPTY_BLOCK_STRUCTURE_ERROR")
private fun RBuilder.resultsTable(testResultDebugInfo: TestResultDebugInfo) = table("table table-bordered") {
    tbody {
        tr {
            td {
                +"Command"
            }
            td {
                small {
                    samp {
                        +(testResultDebugInfo.debugInfo?.execCmd ?: "N/A")
                    }
                }
            }
        }
        tr {
            td {
                +"Test status"
            }
            td {
                val status = testResultDebugInfo.testStatus
                val longMessage: String = when (status) {
                    is Pass -> (status.message ?: "").ifBlank { "Completed successfully without additional information" }
                    is Fail -> status.reason
                    is Ignored -> status.reason
                    is Crash -> status.description
                }
                +"${status::class.simpleName} with message:"
                br { }
                multilineText(longMessage)
            }
        }
        with(testResultDebugInfo.debugInfo!!) {
            listOf(
                "stdout" to ::stdout,
                "stderr" to ::stderr
            )
        }.forEach { (title, getContent) ->
            tr {
                td {
                    +title
                }
                td {
                    small {
                        samp {
                            getContent()?.let(::multilineTextWithIndices)
                                ?: +"N/A"
                        }
                    }
                }
            }
        }
    }
}

private fun RBuilder.fallback(status: String) = div {
    +status
}

/**
 * A component to display details about test execution
 *
 * @return a function component
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION", "TOO_LONG_FUNCTION")
fun testExecutionDetailsView() = fc<Props> {
    val params = useParams()
    val executionId = params["executionId"]!!.toLong()

    val testFilePath = params["*"]!!
    val testResultLocation = TestResultLocation(
        params["testSuiteName"]!!,
        params["pluginName"]!!,
        testFilePath.substringBeforeLast("/", ""),
        testFilePath.substringAfterLast("/"),
    )

    val (status, setStatus) = useState("Loading...")
    val (testResultDebugInfo, setTestResultDebugInfo) = useState<TestResultDebugInfo?>(null)

    // fixme: after https://github.com/saveourtool/save-cloud/issues/364 can be passed via history state to avoid requests
    useRequest(arrayOf(status, testResultDebugInfo, executionId, testResultLocation), isDeferred = false) {
        val testExecutionDtoResponse = post(
            "$apiUrl/testExecutions?executionId=$executionId&checkDebugInfo=true",
            Headers().apply {
                set("Content-Type", "application/json")
            },
            Json.encodeToString(testResultLocation),
            loadingHandler = ::noopLoadingHandler,
        )
        if (testExecutionDtoResponse.ok) {
            val testResultDebugInfoResponse = getDebugInfoFor(testExecutionDtoResponse.decodeFromJsonString())
            if (testResultDebugInfoResponse.ok) {
                setTestResultDebugInfo(
                    testResultDebugInfoResponse.decodeFromJsonString<TestResultDebugInfo>()
                )
            } else {
                setStatus("Additional test info is not available (code ${testResultDebugInfoResponse.status})")
            }
        } else {
            setStatus("Additional test info is not available (code ${testExecutionDtoResponse.status})")
        }
    }()

    testResultDebugInfo?.let {
        resultsTable(testResultDebugInfo)
    }
        ?: run {
            fallback(status)
        }
}
