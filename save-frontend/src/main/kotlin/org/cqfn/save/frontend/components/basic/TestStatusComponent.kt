@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestStatus
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.frontend.externals.fontawesome.faExternalLinkAlt
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon

import okio.ExperimentalFileSystem
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

/**
 * A function component that renders info about [TestResultDebugInfo] into a table [tableInstance]
 *
 * @param testResultDebugInfo data that should be displayed
 * @param tableInstance a table, into which this data is added
 * @return a function component
 */
@Suppress("TOO_LONG_FUNCTION")
@OptIn(ExperimentalFileSystem::class)
fun <D : Any> testStatusComponent(testResultDebugInfo: TestResultDebugInfo, tableInstance: TableInstance<D>) = fc<Props> {
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
    tr("table-sm") {
        td {
            attrs.colSpan = "2"
            +"Executed command"
        }
        td {
            attrs.colSpan = "${numColumns - 2}"
            small {
                samp {
                    +(testResultDebugInfo.debugInfo?.execCmd ?: "N/A")
                }
            }
        }
    }
    tr("table-sm") {
        td {
            attrs.colSpan = "2"
            +"Reason ("
            a(href = "${window.location}/details/$testSuiteName/$pluginName/$testFilePath") {
                +"additional info "
                fontAwesomeIcon(icon = faExternalLinkAlt, classes = "fa-xs")
            }
            +")"
        }
        td {
            attrs.colSpan = "${numColumns - 2}"
            small {
                samp {
                    +shortMessage
                }
            }
        }
    }
}
