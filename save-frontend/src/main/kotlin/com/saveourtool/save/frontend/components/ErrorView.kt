/**
 * A page for errors
 */

package com.saveourtool.save.frontend.components

import com.saveourtool.save.frontend.components.topbar.topBarComponent
import com.saveourtool.save.frontend.components.views.FallbackView
import js.errors.JsError
import react.FC
import react.dom.html.ReactHTML.div
import react.react
import react.router.useRouteError
import web.cssom.ClassName

val errorView = FC {
    val errorMessage = useRouteError().unsafeCast<JsError>().message
    div {
        className = ClassName("container-fluid")
        topBarComponent()
        FallbackView::class.react {
            bigText = "Error"
            smallText = "Something went wrong: $errorMessage"
        }
        @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
        footer { }
    }
}
