/**
 * Utilities to work with react-router
 */

package com.saveourtool.save.frontend.utils

import history.Location
import react.Props
import react.RBuilder
import react.fc
import react.router.Params
import react.router.useLocation
import react.router.useParams

/**
 * Wrapper function component to allow using router props in class components
 * as suggested in https://reactrouter.com/docs/en/v6/faq#what-happened-to-withrouter-i-need-it
 *
 * @param handler router props aware builder
 * @return a function component
 */
@Suppress("TYPE_ALIAS")
fun <T : Props> withRouter(handler: RBuilder.(Location, Params) -> Unit) = fc<T> {
    val location = useLocation()
    val params = useParams()
    handler(location, params)
}
