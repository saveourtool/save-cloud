package org.cqfn.save.info

import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Source data. Each entry represents a chart segment
 *
 * @property title segment name
 * @property value value of segment
 * @property color color of segment
 * @property key custom value to be used as segments element keys
 */
@Serializable
data class DataPieChart(
    val title: String? = null,
    val value: Int,
    var color: String = randomColor(),
    val key: String? = null,
)

/**
 * @return string of random hex color
 */
fun randomColor(): String {
    var stringColor = "#"
    val charPool = "0123456789ABCDEF".split("")
    while (stringColor.length <= 6) {
        stringColor += charPool[Random.nextInt(16)]
    }
    return stringColor
}
