package org.cqfn.save.frontend.components.views

import csstype.BoxDecorationBreak
import csstype.ColorProperty
import csstype.Display
import csstype.FontSize
import csstype.FontWeight
import csstype.TextDecoration
import kotlinx.browser.document
import kotlinx.html.ButtonType
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.dom.a
import react.dom.div
import org.cqfn.save.frontend.components.basic.InputTypes
import org.cqfn.save.frontend.components.basic.inputTextFormRequired
import react.CSSProperties
import react.dom.button
import react.dom.form
import react.dom.h1
import react.dom.h3
import react.dom.h4
import react.dom.main
import react.dom.p
import react.dom.span


/**
 * [RState] of project creation view component
 */
external interface IndexViewState : State {
    var isValidLogin: Boolean?
}

/**
 * A view with collection of projects
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class IndexView : RComponent<PropsWithChildren, IndexViewState>() {
    init {
        state.isValidLogin = true
    }

    @Suppress("ForbiddenComment")
    override fun RBuilder.render() {
        main("main-content mt-0 ps") {
            div("page-header align-items-start min-vh-100") {
                span("mask bg-gradient-dark opacity-6") {}

                div("row") {
                    // Marketing information
                    div("col-lg-4  ml-auto mt-4 mb-5 mr-0 ml-0 text-white") {
                        marketingTitle("Static")
                        marketingTitle("Analysis")
                        marketingTitle("Verification &")
                        marketingTitle("Evaluation")
                        h3("mt-3") {
                            +"Advanced eco-system for continuous integration, evaluation and benchmarking of static analyzers. Powered by"
                            attrs["style"] = kotlinext.js.jsObject<CSSProperties> {
                                color = "#aaacba".unsafeCast<ColorProperty>()
                                display = Display.inline
                                fontSize = "1.8rem".unsafeCast<FontSize>()
                            }
                        }
                        a(classes = "text-info text-gradient font-weight-bold ml-2") {
                            attrs.href = "https://www.huaweicloud.com/"
                            h3 {
                                +"Huawei Cloud."
                                attrs["style"] = kotlinext.js.jsObject<CSSProperties> {
                                    color = "rgb(246 84 21)".unsafeCast<ColorProperty>()
                                    display = Display.inline
                                    textDecoration = "underline rgb(246 84 21);".unsafeCast<TextDecoration>()
                                    boxDecorationBreak = "clone;".unsafeCast<BoxDecorationBreak>()
                                    fontSize = "1.8rem".unsafeCast<FontSize>()

                                }
                            }
                        }
                    }

                    // Sign-in header
                    div("col-lg-3 col-md-8 col-12 mx-auto mt-4 mb-5") {
                        div("card z-index-0 fadeIn3 fadeInBottom") {
                            div("card-header p-0 position-relative mt-n4 mx-3 z-index-2 rounded") {
                                div("bg-info shadow-primary border-radius-lg py-3 pe-1 rounded") {
                                    h4("text-white font-weight-bolder text-center mt-2 mb-0") {
                                        +"Sign in"
                                    }
                                    div("row") {
                                        div("col text-center px-1") {
                                            button(classes = "btn btn-link px-3 text-white text-lg text-center") {
                                                // FixMe: <fa>cking fontAwesome icons do not work here
                                                +"via GITHUB"
                                            }
                                        }
                                    }
                                }
                            }

                            div("card-body") {
                                form(classes = "needs-validation") {
                                    inputTextFormRequired(
                                        InputTypes.LOGIN,
                                        state.isValidLogin!!,
                                        "col-lg ml-0 mr-0 pr-0 pl-0",
                                        "Login"
                                    ) {
                                        // changeFields()
                                    }

                                    inputTextFormRequired(
                                        InputTypes.PASSWORD,
                                        state.isValidLogin!!,
                                        "col-lg ml-0 mr-0 pr-0 pl-0",
                                        "Password"
                                    ) {
                                        // changeFields()
                                    }

                                    div("row text-center") {
                                        button(
                                            type = ButtonType.button,
                                            classes = "btn btn-info w-100 my-4 mb-2 ml-2 mr-2"
                                        ) {
                                            +"Sign in"
                                        }
                                    }


                                    p("mt-4 text-sm text-center") {
                                        +"Don't have an account?"
                                        a(classes = "text-info text-gradient font-weight-bold ml-2") {
                                            attrs.href = "#/"
                                            +"Sign up"
                                        }

                                        p("text-sm text-center") {
                                            +"Or"
                                            a(classes = "text-info text-gradient font-weight-bold ml-2 mr-2") {
                                                attrs.href = "#/projects"
                                                +"Continue"
                                            }
                                            +"without registration"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun RBuilder.marketingTitle(str: String) {
        p("mb-0 mt-0") {
            h1Bold(str[0].toString())
            h1Normal(str.substring(1, str.length))
        }
    }

    private fun RBuilder.h1Bold(str: String) = h1 {
        +str
        attrs["style"] = kotlinext.js.jsObject<CSSProperties> {
            fontWeight = "bold".unsafeCast<FontWeight>()
            display = Display.inline
            fontSize = "4.5rem".unsafeCast<FontSize>()
        }
    }

    private fun RBuilder.h1Normal(str: String) = h1 {
        +str
        attrs["style"] = kotlinext.js.jsObject<CSSProperties> {
            display = Display.inline
        }
    }

    // A small hack to avoid duplication of main content-wrapper from App.kt
    // We will change the background only for sign-up and sign-in views
    override fun componentDidMount() {
        document.getElementById("content-wrapper")?.setAttribute(
            "style",
            "background: -webkit-linear-gradient(270deg, rgb(84, 83, 97), rgb(25, 34, 99), rgb(49, 70, 180))"
        )

        document.getElementById("navigation-top-bar")?.setAttribute(
            "class",
            "navbar navbar-expand navbar-dark topbar mb-3 static-top shadow mr-1 ml-1 rounded"
        )

        document.getElementById("navigation-top-bar")?.setAttribute(
            "style",
            "background: transparent"
        )
    }
}
