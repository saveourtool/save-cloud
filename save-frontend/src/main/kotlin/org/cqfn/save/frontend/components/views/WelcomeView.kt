/**
 * A view related to the sign-in view
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.frontend.components.basic.InputTypes
import org.cqfn.save.frontend.components.basic.inputTextFormRequired
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon

import csstype.ColorProperty
import csstype.Display
import csstype.FontSize
import csstype.FontWeight
import csstype.TextDecoration
import react.CSSProperties
import react.PropsWithChildren
import react.RBuilder
import react.State
import react.dom.a
import react.dom.button
import react.dom.div
import react.dom.form
import react.dom.h1
import react.dom.h3
import react.dom.h4
import react.dom.main
import react.dom.p
import react.dom.span

import kotlinx.html.ButtonType

/**
 * [RState] of project creation view component
 */
external interface IndexViewState : State {
    /**
     * State that checks the validity of login
     */
    var isValidLogin: Boolean?

    /**
     * State that checks the validity of password
     */
    var isValidPassword: Boolean?
}

/**
 * Main entry point view with sign-in page
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class WelcomeView : AbstractView<PropsWithChildren, IndexViewState>(true) {
    init {
        state.isValidLogin = true
        state.isValidPassword = true
    }

    @Suppress("ForbiddenComment", "LongMethod", "TOO_LONG_FUNCTION")
    override fun RBuilder.render() {
        main("main-content mt-0 ps") {
            div("page-header align-items-start min-vh-100") {
                span("mask bg-gradient-dark opacity-6") {}

                div("row") {
                    // Marketing information
                    div("col-lg-4 ml-auto mt-3 mb-5 mr-5 ml-0 text-white") {
                        marketingTitle("Static")
                        marketingTitle("Analysis")
                        marketingTitle("Verification &")
                        marketingTitle("Evaluation")
                        h3("mt-4") {
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
                                    textDecoration = "underline rgb(246 84 21)".unsafeCast<TextDecoration>()
                                    fontSize = "1.8rem".unsafeCast<FontSize>()
                                }
                            }
                        }
                    }

                    // Sign-in header
                    div("col-lg-3 mr-auto ml-5 mt-5 mb-5") {
                        div("card z-index-0 fadeIn3 fadeInBottom") {
                            div("card-header p-0 position-relative mt-n4 mx-3 z-index-2 rounded") {
                                div("bg-info shadow-primary border-radius-lg py-3 pe-1 rounded") {
                                    h4("text-white font-weight-bolder text-center mt-2 mb-0") {
                                        +"Sign in"
                                    }
                                    div("row") {
                                        div("col text-center px-1") {
                                            a(
                                                href = "oauth2/authorization/github",
                                                classes = "btn btn-link px-3 text-white text-lg text-center"
                                            ) {
                                                + "via GitHub"
                                                fontAwesomeIcon {
                                                    attrs.icon = "github"
                                                    attrs.className = "fas fa-lg fa-fw mr-2 text-gray-400"
                                                }
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
                                        state.isValidPassword!!,
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
        div("mb-0 mt-0") {
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
}
