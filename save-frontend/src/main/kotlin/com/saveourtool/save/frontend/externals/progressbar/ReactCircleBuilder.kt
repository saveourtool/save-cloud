@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.externals.progressbar

import com.saveourtool.save.frontend.themes.Colors
import js.core.jso
import react.CSSProperties
import react.ChildrenBuilder
import react.react
import web.cssom.Font
import web.cssom.FontSize

/**
 * @param progress progress and percentage
 * @param size of the circle
 * @param lineWidth of the circle's stroke
 * @param color of percentage text and "progress" portion of circle
 * @param handler
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.progressBar(
    progress: Int,
    size: String = "10rem",
    lineWidth: String = "5rem",
    color: String = Colors.SUCCESS.value,
    showPercentageSymbol: Boolean = false,
    // FixMe: this does not work in Circle, investigate why
    textStyle: CSSProperties = jso {
        font = "bold 6.rem".unsafeCast<Font>()
                                   },
    handler: ChildrenBuilder.(ReactCircleProps) -> Unit = {},
) {
    ReactCircle::class.react {
        this.size = size
        this.lineWidth = lineWidth
        this.progress = progress.toString()
        this.progressColor = color
        this.showPercentageSymbol = showPercentageSymbol
        this.textColor = color
        handler(this)
    }
}
