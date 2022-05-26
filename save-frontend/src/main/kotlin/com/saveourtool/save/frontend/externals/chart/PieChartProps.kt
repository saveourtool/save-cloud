@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.externals.chart

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

/**
 * Source data. Each entry represents a chart segment
 *
 * @property title segment name
 * @property value value of segment
 * @property color color of segment
 * @property key custom value to be used as segments element keys
 */
data class DataPieChart(
    val title: String? = null,
    val value: Int,
    var color: String,
    val key: String? = null,
)
