@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.test.TestSuiteValidationProgress
import csstype.ClassName
import csstype.WhiteSpace
import csstype.Width
import js.core.jso
import react.FC
import react.Props
import react.dom.aria.AriaRole
import react.dom.aria.ariaValueMax
import react.dom.aria.ariaValueMin
import react.dom.aria.ariaValueNow
import react.dom.html.ReactHTML.div

@Suppress(
    "MagicNumber",
    "MAGIC_NUMBER",
)
val testSuiteValidationResultView: FC<TestSuiteValidationResultProps> = FC { props ->
    props.validationResults.forEach { item ->
        div {
            div {
                className = ClassName("progress progress-sm mr-2")
                div {
                    className = ClassName("progress-bar bg-info")
                    role = "progressbar".unsafeCast<AriaRole>()
                    style = jso {
                        width = "${item.percentage}%".unsafeCast<Width>()
                    }
                    ariaValueMin = 0.0
                    ariaValueNow = item.percentage.toDouble()
                    ariaValueMax = 100.0
                }
            }
            div {
                style = jso {
                    whiteSpace = "pre".unsafeCast<WhiteSpace>()
                }

                +item.toString()
            }
        }
    }
}

/**
 * Properties for [testSuiteValidationResultView].
 *
 * @see testSuiteValidationResultView
 */
external interface TestSuiteValidationResultProps : Props {
    /**
     * Test suite validation results.
     */
    var validationResults: Collection<TestSuiteValidationProgress>
}
