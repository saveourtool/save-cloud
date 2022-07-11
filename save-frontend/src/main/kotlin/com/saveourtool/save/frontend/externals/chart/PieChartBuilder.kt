/**
 * kotlin-react builders for PieChart components
 */

package com.saveourtool.save.frontend.externals.chart

import com.saveourtool.save.frontend.externals.fontawesome.FontAwesomeIconProps
import react.*
import kotlin.random.Random

/**
 * @param data dataset for pie chart
 * @param handler handler to set up a component
 * @return ReactElement
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.pieChart(
    data: Array<DataPieChart>,
    handler: ChildrenBuilder.(props: PieChartProps) -> Unit = {},
) = PieChart::class.react.create {
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
