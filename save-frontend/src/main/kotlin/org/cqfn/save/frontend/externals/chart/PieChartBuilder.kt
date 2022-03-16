/**
 * kotlin-react builders for PieChart components
 */

package org.cqfn.save.frontend.externals.chart

import react.RBuilder
import react.RHandler
import kotlin.random.Random

/**
 * @param data dataset for pie chart
 * @param handler handler to set up a component
 * @return ReactElement
 */
@Suppress("MAGIC_NUMBER")
fun RBuilder.pieChart(
    data: Array<DataPieChart>,
    handler: RHandler<PieChartProps> = {},
) = child(PieChart::class) {
    attrs.data = data
    attrs.animate = false
    attrs.segmentsShift = 0
    attrs.viewBoxSize = intArrayOf(100, 100)
    attrs.radius = 50
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
