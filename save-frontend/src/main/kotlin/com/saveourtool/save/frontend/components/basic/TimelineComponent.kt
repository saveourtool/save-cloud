@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import react.*
import react.dom.html.ReactHTML.div
import web.cssom.*

import kotlinx.datetime.LocalDateTime

val timelineComponent: FC<TimelineComponentProps> = FC { props ->
    div {
        className = ClassName("mb-3")
        div {
            className = ClassName("mt-3 mb-3 text-xs text-center font-weight-bold text-primary text-uppercase")
            +"Timeline"
        }
        div {
            className = ClassName("card card-body p-0 timeline-container")
            div {
                className = ClassName("steps-container")
                div {
                    className = ClassName("line completed")
                }
                props.dates.toList()
                    .sortedBy { it.first }
                    .forEach { (dateTime, label) ->
                        div {
                            className = ClassName("step completed")
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
     * Map with dates where key is [LocalDateTime] and value is label
     */
    var dates: Map<LocalDateTime, String>
}
