@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.frontend.externals.chart

import react.PropsWithChildren

/**
 * RProps of [PieChart]
 */
external interface PieChartProps : PropsWithChildren {
    /**
     * Dataset for pie chart
     */
    var data: Array<DataPieChart>
}

/**
 * Source data. Each entry represents a chart segment
 *
 * @property title segment name
 * @property value value of segment
 * @property color color of segment
 */
data class DataPieChart(
    val title: String,
    val value: Int,
    val color: String,
)
