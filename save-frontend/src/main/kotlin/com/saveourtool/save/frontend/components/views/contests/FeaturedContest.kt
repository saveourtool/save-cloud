package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.frontend.externals.fontawesome.faArrowRight
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import csstype.ClassName
import csstype.rem
import kotlinx.js.jso
import react.ChildrenBuilder
import react.dom.html.ButtonType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div

fun ChildrenBuilder.featuredContest() {
        div {
            className = ClassName("col-lg-6")
            div {
                className = ClassName("card flex-md-row mb-1 box-shadow")
                style = jso {
                    height = 14.rem
                }

                ReactHTML.img {
                    className = ClassName("card-img-right flex-auto d-none d-md-block")
                    asDynamic()["data-src"] = "holder.js/200x250?theme=thumb"
                    src = "img/undraw_certificate_re_yadi.svg"
                    asDynamic()["data-holder-rendered"] = "true"
                    style = jso {
                        width = 12.rem
                    }
                }

                div {
                    className = ClassName("card-body d-flex flex-column align-items-start")
                    ReactHTML.strong {
                        className = ClassName("d-inline-block mb-2 text-info")
                        +"Featured Contest"
                    }
                    ReactHTML.h3 {
                        className = ClassName("mb-0")
                        ReactHTML.a {
                            className = ClassName("text-dark")
                            href = "#"
                            +"Contest NAME"
                        }
                    }
                    ReactHTML.p {
                        className = ClassName("card-text mb-auto")
                        +"Contest DESCRIPTION SHORT"
                    }
                    div {
                        className = ClassName("row")
                        ReactHTML.button {
                            type = ButtonType.button
                            className = ClassName("btn btn-sm btn-outline-primary mr-1")
                            onClick = {

                            }
                            +"Register"
                        }

                        ReactHTML.button {
                            type = ButtonType.button
                            className = ClassName("btn btn-sm btn-outline-success")
                            onClick = {

                            }
                            +"Description "
                            fontAwesomeIcon(icon = faArrowRight)
                        }
                    }
                }
            }
        }
}
