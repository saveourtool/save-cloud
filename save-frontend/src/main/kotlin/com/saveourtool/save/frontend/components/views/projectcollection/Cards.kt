package com.saveourtool.save.frontend.components.views.projectcollection

import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes
import csstype.*
import kotlinx.js.jso
import react.ChildrenBuilder
import react.dom.html.ButtonType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p

fun ChildrenBuilder.topRightCard(currentUserInfo: UserInfo?) {
    div {
        className = ClassName("col-lg-6")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 12.rem
            }

            div {
                hidden = (currentUserInfo == null)
                ReactHTML.a {
                    href = "#/${FrontendRoutes.CREATE_PROJECT.path}/"
                    ReactHTML.button {
                        type = ButtonType.button
                        className = ClassName("btn btn-outline-primary mb-2 mr-2")
                        +"Add new tested tool"
                    }
                }
                ReactHTML.a {
                    href = "#/${FrontendRoutes.CREATE_ORGANIZATION.path}/"
                    ReactHTML.button {
                        type = ButtonType.button
                        className = ClassName("btn btn-outline-primary mb-2")
                        +"Add new organization"
                    }
                }
            }
        }
    }
}

fun ChildrenBuilder.topLeftCard() {
    div {
        className = ClassName("col-lg-6")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 12.rem
            }

            div {
                className = ClassName("col-lg-2")

                img {
                    src = "img/save-logo-no-bg.png"
                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }
                }
            }

            div {
                style = jso {
                    justifyContent = JustifyContent.center
                    alignItems = AlignItems.center
                    alignSelf = AlignSelf.center
                }
                className = ClassName("col-lg-9")
                div {
                    className = ClassName("row")

                    h4 {
                        +"Welcome to SAVE!"
                    }
                }
                div {
                    className = ClassName("row")
                    p {
                        +(
                                " This page contains a " +
                                        "list with all public and your private projects created in SAVE. " +
                                        "Your new added tools will also be shown below. Please keep in mind that to create a " +
                                        "new project and add new tested tool you will need to be added or to create a new " +
                                        "organization for benchmarking of your tool."
                                )
                    }
                }
            }
        }
    }
}
