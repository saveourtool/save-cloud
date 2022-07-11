package com.saveourtool.save.frontend.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import react.*
import react.dom.html.ReactHTML.div

/**
 * Base class for react components with CoroutineScope, that will be cancelled on unmounting.
 */
abstract class ComponentWithScope<P : Props, S : State> : Component<P, S>() {
    /**
     * A [CoroutineScope] that should be used by implementing classes. Will be cancelled on unmounting.
     */
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun render(): ReactNode? = div.create {
        render()
    }

    abstract fun ChildrenBuilder.render()

    override fun componentWillUnmount() {
        if (scope.isActive) {
            scope.cancel()
        }
    }
}
