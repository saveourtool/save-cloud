/**
 * Utilities to work with react-router
 */

package com.saveourtool.save.frontend.utils

import history.Location
import react.*
import react.router.*

/**
 * Wrapper function component to allow using router props in class components
 * as suggested in https://reactrouter.com/docs/en/v6/faq#what-happened-to-withrouter-i-need-it
 *
 * @param handler router props aware builder
 * @return a function component
 */
@Suppress("TYPE_ALIAS")
fun <T : Props> withRouter(handler: ChildrenBuilder.(Location, Params) -> Unit) = FC<T> {
    val location = useLocation()
    val params = useParams()
    handler(location, params)
}

interface NavigateFunctionContext {
    val navigate: NavigateFunction
}

fun <T : Props> ChildrenBuilder.withNavigate(handler: ChildrenBuilder.(NavigateFunctionContext) -> Unit) {
    val wrapper = FC<T> {
        val navigate = useNavigate()
        val ctx = object : NavigateFunctionContext {
            override val navigate: NavigateFunction
                get() = navigate
        }
        handler(ctx)
    }
    wrapper()
}
