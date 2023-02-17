@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.externals.prograssbar

import react.ChildrenBuilder
import react.react

private val rad = Pair("rgb(172, 0, 0)", "#ac0000")

private val green = Pair("rgb(0, 213, 0)", "#00d500")

/**
 * @param handler
 * @param progress
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.circle(
    progress: Long,
    handler: ChildrenBuilder.(ReactCircleProps) -> Unit = {},
) {
    val color = if (progress < 51) {
        green
    } else {
        rad
    }

    ReactCircle::class.react {
        this.size = "100"
        this.lineWidth = "50"
        this.progress = progress.toString()
        this.progressColor = color.first
        this.textColor = color.second
        handler(this)
    }
}
