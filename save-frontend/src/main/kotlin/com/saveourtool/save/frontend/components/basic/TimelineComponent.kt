@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.vulnerability.VulnerabilityDateType
import com.saveourtool.save.entities.vulnerability.VulnerabilityDateType.Companion.isSystemDateType
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.frontend.utils.buttonBuilder
import react.*
import react.dom.html.ReactHTML.div
import web.cssom.*

import kotlinx.datetime.LocalDateTime

const val HOVERABLE_CONST = "hoverable"

val timelineComponent: FC<TimelineComponentProps> = FC { props ->
    val hoverable = props.onNodeClick?.let { HOVERABLE_CONST }.orEmpty()

    div {
        className = ClassName("mb-3")
        props.title?.let { title ->
            div {
                className = ClassName("mt-3 mb-3 text-xs text-center font-weight-bold text-primary text-uppercase")
                +title
            }
        }

        props.onAddClick?.let { onClickCallback ->
            buttonBuilder(
                label = "Add date",
                style = "secondary",
                isOutline = true,
                classes = "btn btn-sm btn-primary"
            ) {
                onClickCallback()
            }
        }

        div {
            className = ClassName("p-0 timeline-container")
            div {
                className = ClassName("steps-container")
                div {
                    className = ClassName("line")
                }
                props.dates
                    .plus(
                        VulnerabilityDateType.SUBMITTED.value to
                                (props.vulnerability.creationDateTime ?: LocalDateTime(0, 1, 1, 0, 0, 0, 0))
                    )
                    .toList()
                    .sortedBy { it.second }
                    .forEach { (label, dateTime) ->
                        div {
                            className =
                                    ClassName(if (!label.isSystemDateType()) "step $hoverable" else "step-non-editable")
                            if (!label.isSystemDateType()) {
                                props.onNodeClick?.let { onClickCallback ->
                                    onClick = { onClickCallback(dateTime, label) }
                                }
                            }
                            div {
                                className = ClassName("text-label")
                                +label
                            }
                            div {
                                className = ClassName("date-label")
                                +dateTime.date.toString()
                            }
                        }
                        div {
                            className = ClassName("line")
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
    var dates: Map<String, LocalDateTime>

    /**
     * Callback that should be invoked on add button click
     */
    var onAddClick: (() -> Unit)?

    /**
     * Callback that should be invoked on timeline node click
     */
    @Suppress("TYPE_ALIAS")
    var onNodeClick: ((LocalDateTime, String) -> Unit)?

    /**
     * Vulnerability dto of vulnerability
     */
    var vulnerability: VulnerabilityDto
}
