package org.cqfn.save.frontend.components.basic

import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.domain.TestResultDebugInfo

import react.Props
import react.dom.a
import react.dom.samp
import react.dom.small
import react.dom.td
import react.dom.tr
import react.fc
import react.table.TableInstance

import kotlinx.browser.window

fun <D : Any> testStatusComponent(testResultDebugInfo: TestResultDebugInfo, tableInstance: TableInstance<D>) = fc<Props> {
    // todo: also display execCmd here
    val shortMessage: String = when (val status = testResultDebugInfo.testStatus) {
        is Pass -> status.shortMessage ?: "Completed successfully without additional information"
        is Fail -> status.shortReason
        is Ignored -> status.reason
        is Crash -> status.message
    }
    val numColumns = tableInstance.columns.size
    tr("table-sm") {
        td {
            attrs.colSpan = "2"
            +"Reason"
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
    tr("table-sm") {
        td {
            attrs.colSpan = numColumns.toString()
            +"View additional info "
            val testSuiteName = testResultDebugInfo.testResultLocation.testSuiteName
            val pluginName = testResultDebugInfo.testResultLocation.pluginName
            val testFilePath = with(testResultDebugInfo.testResultLocation) {
                "$testLocation/$testName"
            }
            a(href = "${window.location}/details/$testSuiteName/$pluginName/$testFilePath") {
                +"here"
            }
        }
    }
}
