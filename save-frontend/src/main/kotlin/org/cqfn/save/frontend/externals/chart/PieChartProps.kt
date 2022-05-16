@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.frontend.externals.chart

import org.cqfn.save.info.DataPieChart
import org.w3c.dom.events.Event
import react.PropsWithChildren

/**
 * RProps of [PieChart]
 */
external interface PieChartProps : PropsWithChildren {
    /**
     * Dataset for pie chart
     */
    var data: Array<DataPieChart>

    /**
     * Animate segments on component mount
     */
    var animate: Boolean

    /**
     * Translates segments radially. If number set, provide shift value relative to viewBoxSize space
     */
    var segmentsShift: Int

    /**
     * width and height of SVG viewBox attribute
     */
    var viewBoxSize: IntArray

    /**
     * Radius of the pie (relative to viewBoxSize space)
     */
    var radius: Int

    /**
     * onClick event handler for each segment
     */
    var onClick: (Event, Int) -> Unit

    /**
     * onMouseOut event handler for each segment
     */
    var onMouseOut: (Event, Int) -> Unit

    /**
     * onMouseOver event handler for each segment
     */
    var onMouseOver: (Event, Int) -> Unit
}
