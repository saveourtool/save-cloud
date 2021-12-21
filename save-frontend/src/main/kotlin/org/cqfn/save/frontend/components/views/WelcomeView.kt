/**
 * A view related to the sign-in view
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.frontend.components.basic.InputTypes
import org.cqfn.save.frontend.components.basic.inputTextFormRequired
import org.cqfn.save.frontend.externals.fontawesome.faCopyright
import org.cqfn.save.frontend.externals.fontawesome.faExternalLinkAlt
import org.cqfn.save.frontend.externals.fontawesome.faGithub
import org.cqfn.save.frontend.externals.fontawesome.faSignInAlt
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.info.OauthProviderInfo
import org.cqfn.save.info.UserInfo

import csstype.Display
import csstype.FontSize
import csstype.FontWeight
import org.w3c.fetch.Headers
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
import react.setState

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

    /**
     * List of OAuth providers, that can be accepted by backend
     */
    var oauthProviders: List<OauthProviderInfo>?
}

/**
 * Properties used in WelcomeView (passed from App.kt)
 */
external interface WelcomeProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?
}

/**
 * Main entry point view with sign-in page
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class WelcomeView : AbstractView<WelcomeProps, IndexViewState>(true) {
    init {
        state.isValidLogin = true
        state.isValidPassword = true
    }

    override fun componentDidMount() {
        super.componentDidMount()
        GlobalScope.launch {
            val oauthProviderInfoList: List<OauthProviderInfo>? = get("${window.location.origin}/sec/oauth-providers", Headers()).run {
                if (ok) decodeFromJsonString() else null
            }
            oauthProviderInfoList?.let {
                setState {
                    oauthProviders = it
                }
            }
        }
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
                            +"Advanced eco-system for continuous integration, evaluation and benchmarking of static analyzers."
                        }
                    }

                    // Sign-in header
                    div("col-lg-3 mr-auto ml-5 mt-5 mb-5") {
                        div("card z-index-0 fadeIn3 fadeInBottom") {
                            // if user is not logged in - he needs to input credentials
                            props.userInfo?.let {
                                welcomeUserView()
                            }
                                ?: run {
                                    inputCredentialsView()
                                }
                        }
                    }
                }
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION")
    private fun RBuilder.inputCredentialsView() {
        div("card-header p-0 position-relative mt-n4 mx-3 z-index-2 rounded") {
            div("bg-info shadow-primary border-radius-lg py-3 pe-1 rounded") {
                h4("text-white font-weight-bolder text-center mt-2 mb-0") {
                    +"Sign in"
                }
                div("row") {
                    state.oauthProviders?.map {
                        oauthLogin(it, when (it.registrationId) {
                            "github" -> faGithub
                            "codehub" -> faCopyright
                            else -> faSignInAlt
                        })
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

                    p("text-sm text-center") {
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

    private fun RBuilder.welcomeUserView() {
        div("card-header p-0 position-relative mt-n4 mx-3 z-index-2 rounded") {
            div("bg-info shadow-primary border-radius-lg py-3 pe-1 rounded") {
                h4("text-white font-weight-bolder text-center mt-2 mb-0") {
                    +"Welcome ${props.userInfo?.userName}!"
                }
            }
        }

        div("card-body") {
            p("mt-4 text-sm text-center") {
                a(classes = "text-info text-gradient font-weight-bold ml-2 mr-2") {
                    attrs.href = "#/projects"
                    h4 {
                        +"List of Projects"
                        fontAwesomeIcon(icon = faExternalLinkAlt, "ml-2")
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

    private fun RBuilder.oauthLogin(provider: OauthProviderInfo, icon: dynamic) {
        div("col text-center px-1") {
            a(
                href = provider.authorizationLink,
                classes = "btn btn-link px-3 text-white text-lg text-center"
            ) {
                attrs["style"] = kotlinext.js.jsObject<CSSProperties> {
                    fontSize = "3.2rem".unsafeCast<FontSize>()
                }
                fontAwesomeIcon(icon = icon)
            }
        }
    }
}
