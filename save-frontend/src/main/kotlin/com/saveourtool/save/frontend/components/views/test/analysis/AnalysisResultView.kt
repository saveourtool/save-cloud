@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.test.analysis

import com.saveourtool.save.frontend.externals.fontawesome.faBug
import com.saveourtool.save.frontend.externals.fontawesome.faCheckCircle
import com.saveourtool.save.frontend.externals.fontawesome.faDice
import com.saveourtool.save.frontend.externals.fontawesome.faPoo
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.test.analysis.results.AnalysisResult
import com.saveourtool.save.test.analysis.results.FlakyTest
import com.saveourtool.save.test.analysis.results.IrregularTest
import com.saveourtool.save.test.analysis.results.PermanentFailure
import com.saveourtool.save.test.analysis.results.Regression
import com.saveourtool.save.test.analysis.results.RegularTest
import csstype.ColorProperty
import csstype.MarginRight
import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.abbr

/**
 * Displays a single [AnalysisResult].
 *
 * @see AnalysisResult
 * @see AnalysisResultProps
 */
val analysisResultView: FC<AnalysisResultProps> = FC { props ->
    nowrap("analysisResult") {
        val result = props.analysisResult

        abbr {
            style = jso {
                color = result.iconColor()
                marginRight = "0.25em".unsafeCast<MarginRight>()
            }
            title = result.tooltipText()

            icon(result)
        }

        +result.text()
    }
}

/**
 * Properties of [analysisResultView].
 *
 * @see analysisResultView
 */
external interface AnalysisResultProps : Props {
    /**
     * The analysis result to render.
     */
    var analysisResult: AnalysisResult
}

private fun ChildrenBuilder.icon(result: AnalysisResult): Unit =
        when (result) {
            is RegularTest -> fontAwesomeIcon(icon = faCheckCircle)
            is FlakyTest -> fontAwesomeIcon(icon = faDice)
            is PermanentFailure -> fontAwesomeIcon(icon = faPoo)
            is Regression -> fontAwesomeIcon(icon = faBug)
        }

private fun AnalysisResult.iconColor(): ColorProperty =
        when (this) {
            is RegularTest -> successColor
            is IrregularTest -> failureColor
        }

private fun AnalysisResult.text(): String =
        when (this) {
            is RegularTest -> "Regular test"
            is IrregularTest -> detailMessage
        }

private fun AnalysisResult.tooltipText(): String =
        when (this) {
            is RegularTest -> "Regular Test"
            is FlakyTest -> "Flaky Test"
            is PermanentFailure -> "Permanent Failure"
            is Regression -> "Regression"
        }
