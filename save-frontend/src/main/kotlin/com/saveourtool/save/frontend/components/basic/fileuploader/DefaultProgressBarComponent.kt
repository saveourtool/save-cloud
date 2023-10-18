/**
 * File containing progress bar functional component
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.fileuploader

import js.core.jso
import react.FC
import react.Props
import react.dom.aria.AriaRole
import react.dom.html.ReactHTML.div
import react.useEffect
import web.cssom.ClassName
import web.cssom.Width

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Progress Bar [FC]
 */
@Suppress("MAGIC_NUMBER")
val defaultProgressBarComponent: FC<DefaultProgressBarComponent> = FC { props ->
    val scope = CoroutineScope(Dispatchers.Main)
    useEffect(props.currentProgress) {
        if (props.currentProgress == 100) {
            scope.launch {
                delay(1500.milliseconds)
                props.reset()
            }
        }
    }

    if (props.currentProgress >= 0L) {
        div {
            className = ClassName("progress text-center")
            if (props.currentProgress == 100) {
                className = ClassName("progress-bar bg-success")
                role = "progressbar".unsafeCast<AriaRole>()
                style = jso { width = "100%".unsafeCast<Width>() }
                +props.currentProgresMessage
            } else if (props.currentProgress >= 0) {
                div {
                    className = ClassName("progress-bar progress-bar-striped progress-bar-animated")
                    role = "progressbar".unsafeCast<AriaRole>()
                    style = jso { width = "${props.currentProgress}%".unsafeCast<Width>() }
                    +props.currentProgresMessage
                }
            }
        }
    }
}

/**
 * [Props] for [defaultProgressBarComponent]
 */
external interface DefaultProgressBarComponent : Props {
    /**
     * Current progress
     */
    var currentProgress: Int

    /**
     * Current labelText
     */
    var currentProgresMessage: String

    /**
     * Callback invoked to reset [currentProgress]
     */
    var reset: () -> Unit
}
