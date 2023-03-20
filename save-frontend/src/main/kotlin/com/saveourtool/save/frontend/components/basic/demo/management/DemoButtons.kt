/**
 * Buttons of demo management card
 */

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.utils.buttonBuilder
import csstype.ClassName
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div

/**
 * Display buttons of demo management card
 *
 * @param demoStatus [DemoStatus] of this demo
 * @param userRole current user role, required for button disabling
 * @param sendDemoCreationRequest callback that sends creation request to backend
 * @param getDemoStatus callback that fetches [DemoStatus] of this demo
 * @param startDemo callback that sends request to start demo pod
 * @param stopDemo callback that sends request to stop demo pod
 * @param deleteDemo callback that sends request to delete created demo
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
internal fun ChildrenBuilder.renderButtons(
    demoStatus: DemoStatus,
    userRole: Role,
    sendDemoCreationRequest: () -> Unit,
    getDemoStatus: () -> Unit,
    startDemo: () -> Unit,
    stopDemo: () -> Unit,
    deleteDemo: () -> Unit,
) {
    div {
        className = ClassName("flex-wrap d-flex justify-content-around")
        when (demoStatus) {
            DemoStatus.NOT_CREATED -> buttonBuilder("Create", isDisabled = userRole.isLowerThan(Role.OWNER)) {
                sendDemoCreationRequest()
            }

            DemoStatus.STARTING -> {
                buttonBuilder("Stop", style = "warning", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    stopDemo()
                }
                buttonBuilder("Reload", style = "secondary", isDisabled = userRole.isLowerThan(Role.VIEWER)) {
                    getDemoStatus()
                }
            }

            DemoStatus.RUNNING -> {
                buttonBuilder("Stop", style = "warning", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    stopDemo()
                }
                buttonBuilder("Delete", style = "danger", isDisabled = userRole.isLowerThan(Role.OWNER)) {
                    deleteDemo()
                }
            }

            DemoStatus.ERROR -> {
                buttonBuilder("Run", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    startDemo()
                }
                buttonBuilder("Delete", style = "danger", isDisabled = userRole.isLowerThan(Role.OWNER)) {
                    deleteDemo()
                }
            }

            DemoStatus.STOPPED -> {
                buttonBuilder("Run", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    startDemo()
                }
                buttonBuilder("Update configuration", style = "info", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    // update request here
                }
                buttonBuilder("Delete", style = "danger", isDisabled = userRole.isLowerThan(Role.OWNER)) {
                    deleteDemo()
                }
            }

            DemoStatus.STOPPING -> buttonBuilder(
                "Reload",
                style = "secondary",
                isDisabled = userRole.isLowerThan(Role.VIEWER)
            ) {
                getDemoStatus()
            }
        }
    }
}
