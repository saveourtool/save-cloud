@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.core.result.Crash
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestStatus
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.frontend.externals.fontawesome.faExternalLinkAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import csstype.ClassName

import okio.Path.Companion.toPath
import react.Props
import react.dom.a
import react.dom.samp
import react.dom.small
import react.dom.td
import react.dom.tr
import react.fc
import react.table.TableInstance

import kotlinx.browser.window
import react.FC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.samp
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.tr

/**
 * A function component that renders info about [TestResultDebugInfo] into a table [tableInstance]
 *
 * @param testResultDebugInfo data that should be displayed
 * @param tableInstance a table, into which this data is added
 * @return a function component
 */
@Suppress("TOO_LONG_FUNCTION")
fun <D : Any> testStatusComponent(testResultDebugInfo: TestResultDebugInfo, tableInstance: TableInstance<D>) = FC<Props> {
    val shortMessage: String = when (val status: TestStatus = testResultDebugInfo.testStatus) {
        is Pass -> (status.shortMessage ?: "").ifBlank { "Completed successfully without additional information" }
        is Fail -> status.shortReason
        is Ignored -> status.reason
        is Crash -> status.message
    }
    val numColumns = tableInstance.columns.size
    val testSuiteName = testResultDebugInfo.testResultLocation.testSuiteName
    val pluginName = testResultDebugInfo.testResultLocation.pluginName
    val testFilePath = with(testResultDebugInfo.testResultLocation) {
        testLocation.toPath() / testName
    }
    tr {
        className = ClassName("table-sm")
        td {
            colSpan = 2
            +"Executed command"
        }
        td {
            colSpan = numColumns - 2
            small {
                samp {
                    +(testResultDebugInfo.debugInfo?.execCmd ?: "N/A")
                }
            }
        }
    }
    tr {
        className = ClassName("table-sm")
        td {
            colSpan = 2
            +"Reason ("
            a {
                href = "${window.location}/details/$testSuiteName/$pluginName/$testFilePath"
                +"additional info "
                fontAwesomeIcon(icon = faExternalLinkAlt, classes = "fa-xs")
            }
            +")"
        }
        td {
            colSpan = numColumns - 2
            small {
                samp {
                    +shortMessage
                }
            }
        }
    }
}
