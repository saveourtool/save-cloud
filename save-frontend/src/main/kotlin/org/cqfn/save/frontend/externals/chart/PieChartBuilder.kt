/**
 * kotlin-react builders for PieChart components
 */

package org.cqfn.save.frontend.externals.chart

import react.RBuilder
import react.RHandler

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
