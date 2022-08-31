/**
 * A card with a FEATURED contest (left top card)
 */

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.frontend.externals.fontawesome.faArrowRight
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import csstype.ClassName
import csstype.rem
import react.ChildrenBuilder
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong

import kotlinx.js.jso

/**
 * Rendering of featured contest card
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.featuredContest() {
    div {
        className = ClassName("col-lg-6")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                height = 14.rem
            }

            image()

            div {
                className = ClassName("card-body d-flex flex-column align-items-start")
                strong {
                    className = ClassName("d-inline-block mb-2 text-info")
                    +"Featured Contest"
                }
                h3 {
                    className = ClassName("mb-0")
                    a {
                        className = ClassName("text-dark")
                        href = "#"
                        +"Contest NAME"
                    }
                }
                p {
                    className = ClassName("card-text mb-auto")
                    +"Contest DESCRIPTION SHORT"
                }
                div {
                    className = ClassName("row")
                    button {
                        type = ButtonType.button
                        className = ClassName("btn btn-sm btn-outline-primary mr-1")
                        onClick = {
                            // FixMe: add enrollment logic - modal window
                        }
                        +"Enroll"
                    }

                    button {
                        type = ButtonType.button
                        className = ClassName("btn btn-sm btn-outline-success")
                        onClick = {
                            // FixMe: link or a modal window here?
                        }
                        +"Description "
                        fontAwesomeIcon(icon = faArrowRight)
                    }
                }
            }
        }
    }
}

@Suppress("MAGIC_NUMBER")
private fun ChildrenBuilder.image() {
    img {
        className = ClassName("card-img-right flex-auto d-none d-md-block")
        src = "img/undraw_certificate_re_yadi.svg"
        style = jso {
            width = 12.rem
        }
    }
}
