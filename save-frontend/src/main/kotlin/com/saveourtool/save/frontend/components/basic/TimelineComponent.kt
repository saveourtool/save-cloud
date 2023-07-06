@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.externals.fontawesome.faPlus
import com.saveourtool.save.frontend.utils.buttonBuilder
import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import web.cssom.*

import kotlinx.datetime.LocalDateTime

val timelineComponent: FC<TimelineComponentProps> = FC { props ->
    val hoverable = props.onNodeClick?.let { "hoverable" }.orEmpty()

    div {
        className = ClassName("mb-3")
        props.title?.let { title ->
            div {
                className = ClassName("mt-3 mb-3 text-xs text-center font-weight-bold text-primary text-uppercase")
                +title
            }
        }
        div {
            className = ClassName("p-0 timeline-container")
            div {
                className = ClassName("steps-container")
                div {
                    style = jso {
                        position = "absolute".unsafeCast<Position>()
                        right = "1%".unsafeCast<Right>()
                        top = "0%".unsafeCast<Top>()
                        zIndex = "4".unsafeCast<ZIndex>()
                    }
                    props.onAddClick?.let { onClickCallback ->
                        buttonBuilder(faPlus, style = "secondary", isOutline = true, classes = "rounded-circle btn-sm mt-1 mr-1") {
                            onClickCallback()
                        }
                    }
                }
                div {
                    className = ClassName("line completed")
                }
                props.dates.toList()
                    .sortedBy { it.first }
                    .forEach { (dateTime, label) ->
                        div {
                            className = ClassName("step completed $hoverable")
                            props.onNodeClick?.let { onClickCallback ->
                                style = jso { cursor = "pointer".unsafeCast<Cursor>() }
                                onClick = { onClickCallback(dateTime, label) }
                            }
                            div {
                                className = ClassName("text-label completed")
                                +label
                            }
                            div {
                                className = ClassName("date-label completed")
                                +dateTime.date.toString()
                            }
                        }
                        div {
                            className = ClassName("line completed")
                        }
                    }
                div {
                    className = ClassName("line-end")
                }
            }
        }
    }
}

/**
 * [Props] of [timelineComponent]
 */
external interface TimelineComponentProps : Props {
    /**
     * Timeline title
     */
    var title: String?

    /**
     * Map with dates where key is [LocalDateTime] and value is label
     */
    var dates: Map<LocalDateTime, String>

    /**
     * Callback that should be invoked on add button click
     */
    var onAddClick: (() -> Unit)?

    /**
     * Callback that should be invoked on timeline node click
     */
    @Suppress("TYPE_ALIAS")
    var onNodeClick: ((LocalDateTime, String) -> Unit)?
}
