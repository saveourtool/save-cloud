/**
 * A view related to the sign-in view
 */

@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.frontend.components.errorStatusContext
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.decodeFromJsonString
import com.saveourtool.save.frontend.utils.get
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.info.OauthProviderInfo
import com.saveourtool.save.info.UserInfo

import csstype.Display
import csstype.FontSize
import csstype.FontWeight
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.CSSProperties
import react.Context
import react.PropsWithChildren
import react.RBuilder
import react.RStatics
import react.State
import react.StateSetter
import react.dom.a
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
import kotlinx.coroutines.launch
import kotlinx.js.jso

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
        scope.launch {
            val oauthProviderInfoList: List<OauthProviderInfo>? = get("${window.location.origin}/sec/oauth-providers", Headers(),
                responseHandler = ::noopResponseHandler).run {
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
                        marketingTitle("Software")
                        marketingTitle("Analysis")
                        marketingTitle("Verification &")
                        marketingTitle("Evaluation")
                        h3("mt-4") {
                            +"Advanced eco-system for continuous integration, evaluation and benchmarking of software tools."
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
                div("mt-4 text-sm text-center") {
                    p("mb-0") {
                        +"Don't have an account?"
                    }

                    // Fixme: validateDOMNesting(...): <h4> cannot appear as a descendant of <p>.
                    p("text-sm text-center") {
                        h4 {
                            a(classes = "text-info text-gradient font-weight-bold ml-2 mr-2") {
                                attrs.href = "#/projects"
                                +"Continue "
                                fontAwesomeIcon(icon = faSignInAlt)
                            }
                        }
                        +"with limited functionality"
                    }
                }
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION")
    private fun RBuilder.welcomeUserView() {
        div("card-header p-0 position-relative mt-n4 mx-3 z-index-2 rounded") {
            div("bg-info shadow-primary border-radius-lg py-3 pe-1 rounded") {
                h4("text-white font-weight-bolder text-center mt-2 mb-0") {
                    div("row") {
                        div("col text-center px-1 mb-3") {
                            fontAwesomeIcon(icon = faHome)
                        }
                    }
                    +"Welcome ${props.userInfo?.name}!"
                }
            }
        }

        div("card-body") {
            p("mt-4 text-sm") {
                a(classes = "text-info text-gradient font-weight-bold ml-2 mr-2") {
                    attrs.href = "#/projects"
                    h4 {
                        fontAwesomeIcon(icon = faExternalLinkAlt, "ml-2 mr-2")
                        +"List of Projects"
                    }
                }

                a(classes = "text-info text-gradient font-weight-bold ml-2 mr-2") {
                    attrs.href = "/#/awesome-benchmarks"
                    h4 {
                        fontAwesomeIcon(icon = faFolderOpen, "ml-2 mr-2")
                        +"Benchmarks Archive"
                    }
                }

                a(classes = "text-info text-gradient font-weight-bold ml-2 mr-2") {
                    attrs.href = "/#/${props.userInfo?.name}/settings/email"
                    h4 {
                        fontAwesomeIcon(icon = faUser, "ml-2 mr-2")
                        +"User Settings"
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
        attrs["style"] = jso<CSSProperties> {
            fontWeight = "bold".unsafeCast<FontWeight>()
            display = Display.inline
            fontSize = "4.5rem".unsafeCast<FontSize>()
        }
    }

    private fun RBuilder.h1Normal(str: String) = h1 {
        +str
        attrs["style"] = jso<CSSProperties> {
            display = Display.inline
        }
    }

    private fun RBuilder.oauthLogin(provider: OauthProviderInfo, icon: dynamic) {
        div("col text-center px-1") {
            a(
                href = provider.authorizationLink,
                classes = "btn btn-link px-3 text-white text-lg text-center"
            ) {
                attrs["style"] = jso<CSSProperties> {
                    fontSize = "3.2rem".unsafeCast<FontSize>()
                }
                fontAwesomeIcon(icon = icon)
            }
        }
    }

    companion object : RStatics<WelcomeProps, IndexViewState, WelcomeView, Context<StateSetter<Response?>>>(WelcomeView::class) {
        init {
            contextType = errorStatusContext
        }
    }
}
