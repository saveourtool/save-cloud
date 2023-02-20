@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.externals.prograssbar

import react.ChildrenBuilder
import react.react

/**
 * @property hexColor color in hex format with leading `#` (example: #aabbcc)
 */
enum class Color(val hexColor: String) {
    GREEN("#00d500"),
    RED("#ac0000"),
    ;
}

/**
 * @param progress progress and percentage
 * @param size of the circle
 * @param lineWidth of the circle's stroke
 * @param color of percentage text and "progress" portion of circle
 * @param handler
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.prograssBar(
    progress: Int,
    size: Int = 100,
    lineWidth: Int = 50,
    color: String = Color.GREEN.hexColor,
    handler: ChildrenBuilder.(ReactCircleProps) -> Unit = {},
) {
    ReactCircle::class.react {
        this.size = size.toString()
        this.lineWidth = lineWidth.toString()
        this.progress = progress.toString()
        this.progressColor = color
        this.textColor = color
        handler(this)
    }
}
