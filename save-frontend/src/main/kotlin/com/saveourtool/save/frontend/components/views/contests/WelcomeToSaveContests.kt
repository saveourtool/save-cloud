/**
 * A card with a FEATURED contest (left top card)
 */

package com.saveourtool.save.frontend.components.views.contests

import js.core.jso
import react.ChildrenBuilder
import react.VFC
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import web.cssom.ClassName
import web.cssom.rem

/**
 * Rendering of featured contest card
 */
internal val welcomeToSaveContests = VFC {
    div {
        className = ClassName("col-lg-5")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                @Suppress("MAGIC_NUMBER")
                height = 19.rem
            }
            stayTunedImage()

            div {
                className = ClassName("card-body pb-0 d-flex flex-column align-items-start")
                strong {
                    className = ClassName("d-inline-block mb-2 text-info")
                    +"Welcome to SAVE contests!"
                }
                h3 {
                    className = ClassName("mb-0 text-dark")
                    +"Certification and contests"
                }
                p {
                    className = ClassName("card-text mb-auto")
                    +("On this page you can participate or even propose contests in the area of code analysis. If you would like to participate: ")
                    +("select the contest from ")
                    b {
                        +("active contests > enroll")
                    }
                    +(" to it with your project. ")
                }
            }
        }
    }
}

private fun ChildrenBuilder.stayTunedImage() {
    img {
        className = ClassName("card-img-right flex-auto d-none d-md-block")
        src = "img/undraw_certificate_re_yadi.svg"
        style = jso {
            @Suppress("MAGIC_NUMBER")
            width = 12.rem
        }
    }
}
