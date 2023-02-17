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
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.circle(
    progress: Long,
    handler: ChildrenBuilder.(ReactCircleProps) -> Unit = {},
) {
    val color = if (progress < 51) {
        Color.GREEN.hexColor
    } else {
        Color.RED.hexColor
    }

    ReactCircle::class.react {
        this.size = "100"
        this.lineWidth = "50"
        this.progress = progress.toString()
        this.progressColor = color
        this.textColor = getRgb(color)
        handler(this)
    }
}

@Suppress("MAGIC_NUMBER")
private fun getRgb(hex: String): String {
    val hexString = hex.replace("#", "")
    val first = hexString.substring(0, 2).toInt(16)
    val second = hexString.substring(2, 4).toInt(16)
    val third = hexString.substring(4, 6).toInt(16)
    return "rgb($first, $second, $third)"
}
