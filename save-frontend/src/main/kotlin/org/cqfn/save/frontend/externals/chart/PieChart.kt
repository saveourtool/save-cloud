@file:JsModule("react-minimal-pie-chart")
@file:JsNonModule

package org.cqfn.save.frontend.externals.chart

import react.Component
import react.ReactElement
import react.State

/**
 * External declaration of [PieChart] react component
 */
@JsName("PieChart")
external class PieChart : Component<PieChartProps, State> {
    override fun render(): ReactElement<PieChartProps>?
}
