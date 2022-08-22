/**
 * card with newly added contests
 */

package com.saveourtool.save.frontend.components.views.contests

import csstype.ClassName
import csstype.rem
import react.ChildrenBuilder
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong

import kotlinx.js.jso

/**
 * rendering of newly added contests
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.newContestsCard() {
    div {
        className = ClassName("col-lg-6")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                height = 14.rem
            }

            div {
                className = ClassName("card-body d-flex flex-column align-items-start")
                strong {
                    className = ClassName("d-inline-block mb-2 text-success")
                    +"""New contests"""
                }
                h3 {
                    className = ClassName("mb-0")
                    a {
                        className = ClassName("text-dark")
                        href = "#"
                        +"Hurry up!"
                    }
                }
                p {
                    className = ClassName("card-text mb-auto")
                    +"Checkout and participate in newest contests!"
                }
                a {
                    href = "https://github.com/saveourtool/save-cloud"
                    +"Link "
                }
                a {
                    href = "https://github.com/saveourtool/save"
                    +" Other link"
                }
            }

            img {
                className = ClassName("card-img-right flex-auto d-none d-md-block")
                asDynamic()["data-src"] = "holder.js/200x250?theme=thumb"
                src = "img/undraw_exciting_news_re_y1iw.svg"
                asDynamic()["data-holder-rendered"] = "true"
                style = jso {
                    width = 12.rem
                }
            }
        }
    }
}
