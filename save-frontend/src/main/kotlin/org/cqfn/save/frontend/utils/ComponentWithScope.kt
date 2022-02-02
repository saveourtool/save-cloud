package org.cqfn.save.frontend.utils

import react.Props
import react.RComponent
import react.State

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

/**
 * Base class for react components with CoroutineScope, that will be cancelled on unmounting.
 */
abstract class ComponentWithScope<P : Props, S : State> : RComponent<P, S>() {
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun componentWillUnmount() {
        if (scope.isActive) {
            scope.cancel()
        }
    }
}
