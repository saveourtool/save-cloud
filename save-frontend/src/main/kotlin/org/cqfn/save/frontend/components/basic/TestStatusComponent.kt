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
import react.router.dom.useHistory
import react.table.TableInstance

import kotlinx.browser.window

fun <D : Any> testStatusComponent(testResultDebugInfo: TestResultDebugInfo, tableInstance: TableInstance<D>) = fc<Props> {
    // todo: also display execCmd here
    val shortMessage: String = when (val status = testResultDebugInfo.testStatus) {
        is Pass -> (status.shortMessage ?: "").ifBlank { "Completed successfully without additional information" }
        is Fail -> status.shortReason
        is Ignored -> status.reason
        is Crash -> status.message
    }
    val numColumns = tableInstance.columns.size
    val testSuiteName = testResultDebugInfo.testResultLocation.testSuiteName
    val pluginName = testResultDebugInfo.testResultLocation.pluginName
    val testFilePath = with(testResultDebugInfo.testResultLocation) {
        val path = testLocation.takeIf { it.isNotEmpty() }?.plus('/') ?: ""
        "$path$testName"
    }
    tr("table-sm") {
        td {
            attrs.colSpan = "2"
            +"Reason ("
            a(href = "${window.location}/details/$testSuiteName/$pluginName/$testFilePath") {
                +"additional info"
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
