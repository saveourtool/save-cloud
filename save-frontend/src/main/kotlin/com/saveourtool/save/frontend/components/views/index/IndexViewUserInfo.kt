/**
 * Authorization component (Oauth2 elements) for Index View
 */

package com.saveourtool.save.frontend.components.views.index

import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import web.cssom.*

const val indexViewCustomIconsBackground = "rgb(247, 250, 253)"

val indexViewUserInfo: FC<IndexViewProps> = FC { props ->
    div {
        className = ClassName("row justify-content-center")
        card("Welcome", "Welcome", "img/icon1.png")
        card("Welcome", "Welcome", "img/icon2.png")
        card("Welcome", "Welcome", "img/icon3.png")
    }
}

val userInfo: FC<IndexViewProps> = FC { props ->

}

private fun ChildrenBuilder.card(str: String, title: String, img: String) {
    div {
        className = ClassName("col-3")
        div {
            className = ClassName("row d-flex justify-content-center")

            img {
                src = img
                style = jso {
                    height = 5.rem
                    width = 5.rem
                }
            }
        }

        div {
            className = ClassName("row d-flex justify-content-center")

            h5 {
                style = jso {
                    textAlign = TextAlign.center
                }
                +title
            }
        }

        div {
            className = ClassName("row d-flex justify-content-center")

            p {
                +str
            }
        }
    }
}
