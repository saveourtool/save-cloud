/**
 * Preparation for a participation card
 */

package com.saveourtool.save.frontend.components.views.contests

import csstype.ClassName
import csstype.attr
import csstype.rem
import react.VFC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div

import kotlinx.js.jso
import react.dom.aria.AriaRole
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ol
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span

val yourContests = yourContests()

private fun yourContests() = VFC {
    div {
        className = ClassName("col-lg-8")
        div {
            className = ClassName("carousel slide")
            id = "contestCarousel"
            asDynamic()["data-ride"] = "carousel"
            ol {
                className = ClassName("carousel-indicators")
                li {
                    asDynamic()["data-target"] = "#contestCarousel"
                    asDynamic()["data-slide-to"] = "0"
                }
                li {
                    asDynamic()["data-target"] = "#contestCarousel"
                    asDynamic()["data-slide-to"] = "1"
                }
                li {
                    asDynamic()["data-target"] = "#contestCarousel"
                    asDynamic()["data-slide-to"] = "2"
                }
            }

            div {
                className = ClassName("carousel-inner")
                div {
                    className = ClassName("carousel-item active")

                    img {
                        style = jso {
                            width = 12.rem
                        }
                        alt = "Third slide [800x400]"
                        src = "img/undraw_certificate_re_yadi.svg"
                        asDynamic()["data-holder-rendered"] = "true"
                    }
                }
                div {
                    className = ClassName("carousel-item /*carousel-item-left*/")

                    img {
                        style = jso {
                            width = 12.rem
                        }
                        alt = "Third slide [800x400]"
                        src = "img/undraw_certificate_re_yadi.svg"
                        asDynamic()["data-holder-rendered"] = "true"
                    }
                }
                div {
                    className = ClassName("carousel-item /*carousel-item-next carousel-item-left*/")

                    img {
                        style = jso {
                            width = 12.rem
                        }
                        alt = "Third slide [800x400]"
                        src = "img/undraw_certificate_re_yadi.svg"
                        asDynamic()["data-holder-rendered"] = "true"
                    }
                }
            }
            a {
                className = ClassName("carousel-control-prev")
                href = "#contestCarousel"
                role = AriaRole.button
                asDynamic()["data-slide"] = "prev"
                span {
                    className = ClassName("carousel-control-prev-icon")
                    asDynamic()["aria-hidden"] = "true"
                }
                span {
                    className = ClassName("sr-only")
                    +"""Previous"""
                }
            }
            a {
                className = ClassName("carousel-control-next")
                href = "#contestCarousel"
                role = AriaRole.button
                asDynamic()["data-slide"] = "next"
                span {
                    className = ClassName("carousel-control-next-icon")
                    asDynamic()["aria-hidden"] = "true"
                }
                span {
                    className = ClassName("sr-only")
                    +"""Next"""
                }
            }
        }
    }
}
