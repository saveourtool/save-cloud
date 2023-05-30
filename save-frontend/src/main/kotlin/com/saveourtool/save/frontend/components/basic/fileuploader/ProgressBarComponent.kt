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
val progressBarComponent: FC<ProgressBarComponentProps> = FC { props ->
    val scope = CoroutineScope(Dispatchers.Main)
    useEffect(props.current) {
        if (props.total == props.current && props.total != 0L) {
            scope.launch {
                delay(1500.milliseconds)
                props.flushCounters()
            }
        }
    }

    if (props.total != 0L) {
        div {
            className = ClassName("progress text-center")
            if (props.current < props.total) {
                div {
                    className = ClassName("progress-bar progress-bar-striped progress-bar-animated")
                    role = "progressbar".unsafeCast<AriaRole>()
                    style = jso { width = "${(100 * props.current / props.total).toInt()}%".unsafeCast<Width>() }
                    +props.getLabelText(props.current, props.total)
                }
            } else if (props.current == props.total) {
                className = ClassName("progress-bar bg-success")
                role = "progressbar".unsafeCast<AriaRole>()
                style = jso { width = "100%".unsafeCast<Width>() }
                +"Successfully uploaded ${(props.total / 1024)} KB."
            }
        }
    }
}

/**
 * [Props] for [progressBarComponent]
 */
external interface ProgressBarComponentProps : Props {
    /**
     * Amount of entity that is already marked as done (uploaded, downloaded, fixed, etc.)
     */
    var current: Long

    /**
     * Total amount of entity
     */
    var total: Long

    /**
     * Callback that returns pretty-printed stats (current / total)
     */
    @Suppress("TYPE_ALIAS")
    var getLabelText: (Long, Long) -> String

    /**
     * Callback invoked to flush [current] and [total]
     */
    var flushCounters: () -> Unit
}
