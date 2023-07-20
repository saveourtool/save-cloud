/**
 * File containing utils for welcome views rendering
 */

package com.saveourtool.save.frontend.components.views.welcome

import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.themes.Colors
import com.saveourtool.save.frontend.utils.OauthProvidersFeConfig
import com.saveourtool.save.frontend.utils.processRegistrationId
import com.saveourtool.save.info.OauthProviderInfo
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.p
import react.router.dom.Link
import web.cssom.*

/**
 * @param oauthProviders
 * @param primaryColor color of a shield
 * @param continueLink link for `continue` button
 */
@Suppress("TOO_LONG_FUNCTION")
internal fun ChildrenBuilder.inputCredentialsView(
    oauthProviders: List<OauthProviderInfo>,
    primaryColor: Colors,
    continueLink: String,
) {
    div {
        className = ClassName("card-header p-0 position-relative mt-n4 mx-3 z-index-2 rounded")
        div {
            className = ClassName("shadow-primary border-radius-lg py-3 pe-1 rounded")
            style = jso {
                backgroundColor = primaryColor.value.unsafeCast<BackgroundColor>()
            }
            h4 {
                className = ClassName("text-white font-weight-bolder text-center mt-2 mb-3")
                +"Sign in"
            }
            div {
                className = ClassName("row")
                oauthProviders.map {
                    processRegistrationId(
                        OauthProvidersFeConfig(
                            size = @Suppress("MAGIC_NUMBER") 3.rem,
                            it,
                        )
                    )
                }
            }
        }
    }

    div {
        className = ClassName("card-body")
        form {
            className = ClassName("needs-validation")
            div {
                className = ClassName("mt-4 text-sm text-center")
                p {
                    className = ClassName("mb-0")
                    +"Don't have an account?"
                }

                div {
                    className = ClassName("text-sm text-center")
                    h4 {
                        style = jso {
                            color = "#3075c0".unsafeCast<Color>()
                        }
                        Link {
                            to = continueLink
                            className = ClassName("text-gradient font-weight-bold ml-2 mr-2")
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

/**
 * Render nice menu with options built from [renderMenu]
 *
 * @param userInfo current [UserInfo]
 * @param primaryColor color of `Welcome, {username}` shield
 * @param renderMenu callback to render menu options
 */
internal fun ChildrenBuilder.welcomeUserMenu(
    userInfo: UserInfo?,
    primaryColor: Colors,
    renderMenu: ChildrenBuilder.() -> Unit
) {
    div {
        className = ClassName("card-header p-0 position-relative mt-n4 mx-3 z-index-2 rounded")
        div {
            className = ClassName("shadow-primary border-radius-lg py-3 pe-1 rounded")
            style = jso {
                backgroundColor = primaryColor.value.unsafeCast<BackgroundColor>()
            }
            h4 {
                className = ClassName("text-white font-weight-bolder text-center mt-2 mb-0")
                div {
                    className = ClassName("row")
                    div {
                        className = ClassName("col text-center px-1 mb-3")
                        Link {
                            className = ClassName("text-gradient font-weight-bold ml-2 mr-2")
                            to = "/${FrontendRoutes.INDEX}"
                            fontAwesomeIcon(icon = faHome) {
                                it.color = "#FFFFFF"
                            }
                        }
                    }
                }
                +"Welcome, ${userInfo?.name}!"
            }
        }
    }

    div {
        className = ClassName("card-body")
        div {
            className = ClassName("text-sm")
            renderMenu()
            hrNoMargin()
            menuTextAndLink("Go to main page", FrontendRoutes.INDEX, faHome)
        }
    }
}

/**
 * Render styled [text] with link by [route]'s [FrontendRoutes.path] and leading [icon]
 *
 * @param text [String] to display
 * @param route that menu options points to
 * @param icon [FontAwesomeIcon] to display
 */
internal fun ChildrenBuilder.menuTextAndLink(text: String, route: FrontendRoutes, icon: FontAwesomeIconModule) {
    Link {
        className = ClassName("text-gradient font-weight-bold ml-2 mr-2")
        to = "/$route"
        h4 {
            style = jso {
                color = "#3075c0".unsafeCast<Color>()
                marginBottom = "0.0em".unsafeCast<Margin>()
            }
            fontAwesomeIcon(icon = icon, "ml-2 mr-2")
            +text
        }
    }
}

/**
 * Render horizontal line with [Margin] equals to `0.0em`
 */
internal fun ChildrenBuilder.hrNoMargin() {
    hr {
        style = jso {
            marginTop = "0.0em".unsafeCast<Margin>()
            marginBottom = "0.0em".unsafeCast<Margin>()
        }
    }
}
