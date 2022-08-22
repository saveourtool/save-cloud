/**
 * Contains utils method for React
 */

package com.saveourtool.save.frontend.utils

import react.ChildrenBuilder
import react.useState

/**
 * Runs the provided [action] only once of first render
 *
 * @param action
 */
@Suppress("unused")
fun ChildrenBuilder.runOnlyOnFirstRender(action: () -> Unit) {
    val (isFirstRender, setFirstRender) = useState(true)
    if (isFirstRender) {
        action()
        setFirstRender(false)
    }
}

/**
 * Can only be called from functional components
 * @param updateNotificationMessage callback to show notification message
 * @return current value and callback for showGlobalRoleWarning
 */
fun createGlobalRoleWarningCallback(updateNotificationMessage: (String, String) -> Unit): Pair<Boolean, () -> Unit> {
    val (wasConfirmationModalShown, setWasConfirmationModalShown) = useState(false)
    val showGlobalRoleWarning = {
        updateNotificationMessage(
            "Super admin message",
            "Keep in mind that you are super admin, so you are able to manage organization regardless of your organization permissions.",
        )
        setWasConfirmationModalShown(true)
    }
    return wasConfirmationModalShown to showGlobalRoleWarning
}
