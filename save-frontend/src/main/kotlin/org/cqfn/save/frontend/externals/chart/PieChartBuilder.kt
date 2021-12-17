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
fun RBuilder.pieChart(
    data: Array<DataPieChart>,
    handler: RHandler<PieChartProps> = {},
) = child(PieChart::class) {
    attrs.data = data
    handler(this)
}
