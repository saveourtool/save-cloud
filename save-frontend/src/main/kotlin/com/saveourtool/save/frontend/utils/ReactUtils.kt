/**
 * Contains utils method for React
 */

package com.saveourtool.save.frontend.utils

import react.useState

/**
 * Runs the provided [action] only once of first render
 *
 * @param action
 */
fun runOnlyOnFirstRender(action: () -> Unit) {
    val (isFirstRender, setFirstRender) = useState(true)
    if (isFirstRender) {
        action()
        setFirstRender(false)
    }
}
