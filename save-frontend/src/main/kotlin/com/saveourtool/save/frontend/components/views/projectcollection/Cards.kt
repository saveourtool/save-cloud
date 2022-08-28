package com.saveourtool.save.frontend.components.views.projectcollection

import com.saveourtool.save.validation.FrontendRoutes
import csstype.*
import kotlinx.js.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p

fun ChildrenBuilder.topRightCard() {
    div {
        className = ClassName("col-lg-3")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                borderWidth = 0.2.rem
                borderColor = "#0275d8".unsafeCast<BorderColor>()
            }

            div {
                className = ClassName("col-lg-6 link-container")
                style = jso {
                    minHeight = 12.rem
                }

                a {
                    className = ClassName("link-three ml-5")
                    href = "#/${FrontendRoutes.CREATE_ORGANIZATION.path}/"
                    style = jso {
                        textDecoration = "none".unsafeCast<TextDecoration>()
                    }
                    h3 {
                        +"Create"
                    }
                    h3 {
                        +"new"
                    }
                    h3 {
                        +"organization"
                    }
                }
            }

            div {
                className = ClassName("col-lg-6")
                style = jso {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }
                img {
                    src = "img/undraw_for_review_eqxk.svg"
                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }
                }
            }
        }
    }

    div {
        className = ClassName("col-lg-3")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                borderWidth = 0.2.rem
                borderColor = "#0275d8".unsafeCast<BorderColor>()
            }

            div {
                className = ClassName("col-lg-6 link-container")
                style = jso {
                    minHeight = 12.rem
                    textDecoration = "none".unsafeCast<TextDecoration>()
                }

                a {
                    className = ClassName("link-three")
                    href = "#/${FrontendRoutes.CREATE_PROJECT.path}/"
                    style = jso {
                        textDecoration = "none".unsafeCast<TextDecoration>()
                    }
                    h3 {
                        +"Create"
                    }
                    h3 {
                        +"new"
                    }
                    h3 {
                        +"project"
                    }
                }
            }

            div {
                className = ClassName("col-lg-6")
                style = jso {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }
                img {
                    src = "img/undraw_selecting_team_re_ndkb.svg"
                    style = jso {
                        width = "100%".unsafeCast<Width>()
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
                minHeight = 12.5.rem
            }

            div {
                className = ClassName("col-lg-3 mt-3")

                img {
                    src = "img/save-logo-no-bg.png"
                    style = jso {
                        width = "100%".unsafeCast<Width>()
                    }
                }
            }

            div {
                className = ClassName("col-lg-8 mt-3")

                style = jso {
                    justifyContent = JustifyContent.center
                    alignItems = AlignItems.center
                    alignSelf = AlignSelf.center
                }
                div {
                    className = ClassName("row")

                    h4 {
                        className = ClassName("text-info")
                        +"Welcome to SAVE!"
                    }
                }
                div {
                    className = ClassName("row")
                    p {
                        +(
                                "This page contains a " +
                                        "list with all public and your private projects created in SAVE. " +
                                        "Your new added tools will also be shown below. To create a " +
                                        "new project and to evaluate your tool with benchmarks you will need to have " +
                                        "an organization created. "
                                )
                        b {
                            +"To participate in contests you also need to have a project."
                        }
                    }

                }
            }
        }
    }
}
