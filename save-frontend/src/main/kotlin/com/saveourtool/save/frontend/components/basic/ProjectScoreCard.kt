@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import csstype.*
import react.Props
import react.dom.aria.AriaRole
import react.dom.aria.ariaValueMax
import react.dom.aria.ariaValueMin
import react.dom.aria.ariaValueNow
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.fc

import kotlinx.js.jso

/**
 * ProjectScoreCardProps component props
 */
external interface ProjectScoreCardProps : Props {
    /**
     * Name of a current project
     */
    var projectName: String

    /**
     * Score of a project in a contest
     */
    var contestScore: Long
}

/**
 * Functional component for project score demonstration
 *
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun projectScoreCard() = fc<ProjectScoreCardProps> { props ->
    div {
        attrs.className = ClassName("card border-left-info shadow h-70 py-2")
        div {
            attrs.className = ClassName("card-body")
            div {
                attrs.className = ClassName("row no-gutters align-items-center")
                div {
                    attrs.className = ClassName("col-12 row mr-2")
                    attrs.style = jso {
                        justifyContent = JustifyContent.spaceAround
                        display = Display.flex
                        alignItems = AlignItems.center
                    }
                    div {
                        attrs.className = ClassName("col-1")
                        div {
                            attrs.className = ClassName("text-xs font-weight-bold text-info text-uppercase mb-1 ml-2")
                            attrs.style = jso {
                                justifyContent = JustifyContent.center
                                display = Display.flex
                                alignItems = AlignItems.center
                                alignSelf = AlignSelf.start
                            }
                            +"Rating"
                        }
                        div {
                            attrs.className = ClassName("text-center h5 mb-0 font-weight-bold text-gray-800 mt-1 ml-2")
                            attrs.style = jso {
                                justifyContent = JustifyContent.center
                                display = Display.flex
                                alignItems = AlignItems.center
                                alignSelf = AlignSelf.start
                            }
                            +"${props.contestScore}"
                        }
                    }
                    div {
                        attrs.className = ClassName("col-10")
                        div {
                            h6 {
                                attrs.style = jso {
                                    justifyContent = JustifyContent.center
                                    display = Display.flex
                                    alignItems = AlignItems.center
                                    alignSelf = AlignSelf.center
                                }
                                +props.projectName
                            }
                        }
                        div {
                            attrs.className = ClassName("progress progress-sm mr-2")
                            div {
                                attrs.className = ClassName("progress-bar bg-info")
                                attrs.role = AriaRole.progressbar
                                attrs.style = jso {
                                    width = "${props.contestScore}%".unsafeCast<Width>()
                                }
                                attrs.ariaValueNow = props.contestScore.toDouble()
                                attrs.ariaValueMin = 0.0
                                attrs.ariaValueMax = 100.0
                            }
                        }
                    }
                }
            }
        }
    }
}
