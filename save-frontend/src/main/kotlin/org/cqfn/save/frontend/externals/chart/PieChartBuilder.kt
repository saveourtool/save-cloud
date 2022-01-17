/**
 * kotlin-react builders for PieChart components
 */

package org.cqfn.save.frontend.externals.chart

import org.w3c.dom.events.Event
import react.RBuilder
import react.RHandler

/**
 * @param data dataset for pie chart
 * @param animate animate segments on component mount
 * @param segmentsShift translates segments radially. If number set, provide shift value relative to viewBoxSize space
 * @param viewBoxSize width and height of SVG viewBox attribute
 * @param radius radius of the pie (relative to viewBoxSize space)
 * @param handler handler to set up a component
 * @param onClick event handler for each segment
 * @param onMouseOut event handler for each segment
 * @param onMouseOver event handler for each segment
 * @return ReactElement
 */
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
    "MAGIC_NUMBER",
    "LAMBDA_IS_NOT_LAST_PARAMETER",
)
fun RBuilder.pieChart(
    data: Array<DataPieChart>,
    animate: Boolean = false,
    segmentsShift: Int = 0,
    viewBoxSize: IntArray = intArrayOf(100, 100),
    radius: Int = 50,
    onClick: (Event, Int) -> Unit,
    onMouseOut: (Event, Int) -> Unit,
    onMouseOver: (Event, Int) -> Unit,
    handler: RHandler<PieChartProps> = {},
) = child(PieChart::class) {
    attrs.data = data
    attrs.animate = animate
    attrs.segmentsShift = segmentsShift
    attrs.viewBoxSize = viewBoxSize
    attrs.radius = radius
    attrs.onClick = onClick
    attrs.onMouseOut = onMouseOut
    attrs.onMouseOver = onMouseOver
    handler(this)
}
