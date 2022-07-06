/**
 * Support for [error boundaries](https://reactjs.org/docs/error-boundaries.html)
 */

package com.saveourtool.save.frontend.components

import com.saveourtool.save.frontend.components.views.FallbackView
import com.saveourtool.save.frontend.topBarComponent

import csstype.ClassName
import react.Component
import react.PropsWithChildren
import react.RStatics
import react.ReactNode
import react.State
import react.create
import react.dom.html.ReactHTML.div
import react.react

import kotlinx.js.jso

/**
 * State of error boundary component
 */
external interface ErrorBoundaryState : State {
    /**
     * True is there is an error in the wrapped component tree
     */
    var hasError: Boolean?
}

/**
 * Component to act as React Error Boundary
 */
class ErrorBoundary : Component<PropsWithChildren, ErrorBoundaryState>() {
    init {
        state = jso {
            hasError = false
        }
    }

    override fun render(): ReactNode? = if (state.hasError == true) {
        div.create {
            className = ClassName("container-fluid")
            topBarComponent()
            FallbackView::class.react {
                bigText = "Error"
                smallText = "Something went wrong"
            }
            Footer::class.react()
        }
    } else {
        props.children
    }

    companion object : RStatics<PropsWithChildren, ErrorBoundaryState, ErrorBoundary, Nothing>(ErrorBoundary::class) {
        init {
            /*
             * From [React docs](https://reactjs.org/docs/error-boundaries.html):
             * 'A class component becomes an error boundary if it defines either (or both) of the lifecycle methods static getDerivedStateFromError() or componentDidCatch()'
             */
            getDerivedStateFromError = {
                jso {
                    hasError = true
                }
            }
        }
    }
}
