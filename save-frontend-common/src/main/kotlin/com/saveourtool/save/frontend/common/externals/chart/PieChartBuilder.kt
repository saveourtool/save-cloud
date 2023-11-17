/**
 * kotlin-react builders for PieChart components
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.common.externals.chart

import react.ChildrenBuilder
import react.react
import kotlin.random.Random

/**
 * @property hex
 */
enum class PieChartColors(val hex: String) {
    GREEN("#89E894"),
    GREY("#CCCCC4"),
    RED("#FF8989"),
    ;
}

/**
 * @param data dataset for pie chart
 * @param handler handler to set up a component
 * @return ReactElement
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.pieChart(
    data: Array<DataPieChart>,
    handler: ChildrenBuilder.(PieChartProps) -> Unit = {},
) = PieChart::class.react {
    this.data = data
    animate = false
    segmentsShift = 0
    viewBoxSize = intArrayOf(100, 100)
    radius = 50
    handler(this)
}

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
