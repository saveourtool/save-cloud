@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.externals.prograssbar

import react.ChildrenBuilder
import react.react

/**
 * @property hexColor
 */
enum class Color(val hexColor: String) {
    GREEN("#00d500"),
    RED("#ac0000"),
    ;
}

/**
 * @param handler
 * @param progress
 * @param size
 * @param lineWidth
 * @param color
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
