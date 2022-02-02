@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.frontend.components.views

import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.domain.TestResultLocation
import org.cqfn.save.frontend.http.getDebugInfoFor
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.multilineText
import org.cqfn.save.frontend.utils.multilineTextWithIndices
import org.cqfn.save.frontend.utils.post

import org.w3c.fetch.Headers
import react.Cleanup
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
import react.useEffect
import react.useState

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    val scope = CoroutineScope(Dispatchers.Default)
    // fixme: after https://github.com/analysis-dev/save-cloud/issues/364 can be passed via history state to avoid requests
    useEffect(listOf<dynamic>(executionId, testResultLocation)) {
        scope.launch {
            val testExecutionDtoResponse = post(
                "$apiUrl/testExecutions?executionId=$executionId",
                Headers().apply {
                    set("Content-Type", "application/json")
                },
                Json.encodeToString(testResultLocation)
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
        }
        this.unsafeCast<Array<Cleanup>>().run {
            set(lastIndex + 1) {
                if (scope.isActive) {
                    scope.cancel()
                }
            }
        }
    }

    testResultDebugInfo?.let {
        resultsTable(testResultDebugInfo)
    }
        ?: run {
            fallback(status)
        }
}
