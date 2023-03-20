/**
 * Current demo status
 */

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.save.demo.DemoStatus
import csstype.ClassName
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label

/**
 * Display current demo status wrapped into bordered colorful pill
 *
 * @param demoStatus current status of demo e.g. [DemoStatus.STOPPED], [DemoStatus.RUNNING] etc.
 */
internal fun ChildrenBuilder.renderStatusLabel(demoStatus: DemoStatus) {
    div {
        className = ClassName("col-6 d-flex justify-content-center")
        div {
            val borderStyle = when (demoStatus) {
                DemoStatus.NOT_CREATED -> "border-dark"
                DemoStatus.STARTING, DemoStatus.STOPPING -> "border-warning"
                DemoStatus.RUNNING -> "border-success"
                DemoStatus.STOPPED -> "border-secondary"
                DemoStatus.ERROR -> "border-danger"
            }
            className =
                    ClassName("border $borderStyle d-flex align-items-center justify-content-between rounded-pill m-3")
            div {
                className = ClassName("col m-3 flex-wrap")
                label {
                    className = ClassName("m-0")
                    +"Status"
                }
            }
            div {
                className = ClassName("col m-3 flex-wrap")
                label {
                    className = ClassName("m-0")
                    +demoStatus.name
                }
            }
        }
    }
}
