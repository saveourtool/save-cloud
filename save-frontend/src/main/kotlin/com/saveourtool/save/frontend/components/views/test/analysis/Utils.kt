@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.test.analysis

import js.core.JsoDsl
import js.core.jso
import react.CSSProperties
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.abbr
import react.dom.html.ReactHTML.span
import web.cssom.ClassName
import web.cssom.ColorProperty
import web.cssom.FontWeight
import web.cssom.PaddingLeft
import web.cssom.WhiteSpace

internal const val FLIP_RATE_DESCRIPTION: String = """
A flip is a test status change ${'\u2014'} either from "PASSED" to "FAILED", or vice versa.
The Flip Rate is the ratio of such "flips" to the invocation count of a given test.

A test which constantly fails, while having a 100% failure rate,
will have its flip rate close to zero;
a test which "flips" each time it is invoked will have the flip rate close to 100%.

If the flip rate is too high, the test will be considered flaky.
"""

internal val listStyle: CSSProperties = jso {
    paddingLeft = "1.0em".unsafeCast<PaddingLeft>()
}

/**
 * The placeholder displayed when there's no data available.
 */
internal val noDataAvailable = FC {
    span {
        className = ClassName("noDataAvailable")

        +"No data available"
    }
}

internal val successColor: ColorProperty = "#59a869".unsafeCast<ColorProperty>()

internal val failureColor: ColorProperty = "#a90f1a".unsafeCast<ColorProperty>()

/**
 * A value with a label and an optional tooltip.
 *
 * @see LabelledValueProps
 */
internal val labelledValue: FC<LabelledValueProps> = FC { props ->
    nowrap {
        span {
            className = ClassName("label")

            props.labelTooltip?.let { labelTooltip ->
                abbr {
                    title = labelTooltip
                    +props.label
                }
            } ?: +props.label
        }

        +": "

        boldText(props.value)
    }
}

/**
 * Properties of [labelledValue].
 *
 * @see labelledValue
 */
internal external interface LabelledValueProps : Props {
    /**
     * The label.
     */
    var label: String

    /**
     * The optional tooltip which explains the meaning of the [label].
     */
    var labelTooltip: String?

    /**
     * The value to be displayed.
     */
    var value: Any
}

/**
 * Renders the [block] in a single line, w/o wrapping.
 *
 * @param block the block to render.
 * @param classNames optional extra CSS class names.
 */
internal fun ChildrenBuilder.nowrap(
    vararg classNames: String,
    block: ChildrenBuilder.() -> Unit
) {
    span {
        style = jso {
            whiteSpace = WhiteSpace.nowrap
        }
        className = ClassName(classNames.joinToString(separator = " "))

        block()
    }
}

/**
 * Renders the [text] in bold, optionally applying an [extra style][extraStyle].
 *
 * @param text the text to render.
 * @param extraStyle an optional extra CSS style.
 */
internal fun ChildrenBuilder.boldText(
    text: Any,
    extraStyle: @JsoDsl CSSProperties.() -> Unit = {},
): Unit =
        span {
            style = jso {
                fontWeight = FontWeight.bold
                extraStyle()
            }

            +text.toString()
        }
