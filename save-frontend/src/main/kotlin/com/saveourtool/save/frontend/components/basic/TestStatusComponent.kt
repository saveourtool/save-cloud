@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.core.result.Crash
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.core.result.TestStatus
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.frontend.components.tables.visibleColumnsCount
import com.saveourtool.save.frontend.externals.fontawesome.faExternalLinkAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.samp
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.tr
import tanstack.table.core.Table
import web.cssom.ClassName

import kotlinx.browser.window

/**
 * A function component that renders info about [TestResultDebugInfo] into a table [tableInstance]
 *
 * @param testResultDebugInfo data that should be displayed
 * @param tableInstance a table, into which this data is added
 * @return a function component
 */
@Suppress("TOO_LONG_FUNCTION")
fun <D : Any> testStatusComponent(testResultDebugInfo: TestResultDebugInfo, tableInstance: Table<D>) = FC<Props> {
    val shortMessage: String = when (val status: TestStatus = testResultDebugInfo.testStatus) {
        is Pass -> (status.shortMessage ?: "").ifBlank { "Completed successfully without additional information" }
        is Fail -> status.shortReason
        is Ignored -> status.reason
        is Crash -> status.message
    }
    val numColumns = tableInstance.visibleColumnsCount()
    val testSuiteName = testResultDebugInfo.testResultLocation.testSuiteName
    val pluginName = testResultDebugInfo.testResultLocation.pluginName
    val testPath = testResultDebugInfo.testResultLocation.testPath
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
                // Trim location if it is present some filter at the end, like `?status=PASSED`,
                // it is the situation, when user got to this page by clicking corresponding table column on history view
                val baseLocation = window.location.toString().substringBefore('?')
                href = "$baseLocation/details/$testSuiteName/$pluginName/$testPath"
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

/**
 * A function component that renders info about execution [failReason] into a table [tableInstance]
 *
 * @param failReason reason of execution fail
 * @param tableInstance a table, into which this data is added
 * @return a function component
 */
fun <D : Any> executionStatusComponent(
    failReason: String,
    tableInstance: Table<D>
) = FC<Props> {
    tr {
        td {
            colSpan = 2
            +"Execution fail reason:"
        }
        td {
            colSpan = tableInstance.visibleColumnsCount() - 2
            small {
                samp {
                    +failReason
                }
            }
        }
    }
}
