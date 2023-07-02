package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.externals.fontawesome.faCopyright
import com.saveourtool.save.frontend.externals.fontawesome.faGithub
import com.saveourtool.save.frontend.externals.fontawesome.faSignInAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.info.OauthProviderInfo
import com.saveourtool.save.info.UserInfo
import js.core.jso
import kotlinx.browser.window
import org.w3c.fetch.Headers
import react.ChildrenBuilder
import react.VFC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.useState
import web.cssom.BackgroundColor
import web.cssom.ClassName
import web.cssom.FontSize

val indexAuth: VFC = VFC {
    val (oauthProviders, setOauthProviders) = useState(emptyList<OauthProviderInfo>())
    val getOauthProviders = useDeferredRequest {
        val oauthProviderInfoList: List<OauthProviderInfo>? = get(
            "${window.location.origin}/sec/oauth-providers",
            Headers(),
            loadingHandler =  ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        ).run {
            if (ok) decodeFromJsonString() else null
        }

        oauthProviderInfoList?.let {
            setOauthProviders (oauthProviders)
        }
    }

        getOauthProviders()


    div {
        className = ClassName("card-header p-0 position-relative mt-n4 mx-3 z-index-2 rounded")
        div {
            className = ClassName("shadow-primary border-radius-lg py-3 pe-1 rounded")
            style = jso {
                backgroundColor = "#3075c0".unsafeCast<BackgroundColor>()
            }
            h4 {
                className = ClassName("text-white font-weight-bolder text-center mt-2 mb-0")
                +"Sign in"
            }
            div {
                className = ClassName("row")
                oauthProviders.map {
                    oauthLogin(
                        it, when (it.registrationId) {
                            "github" -> faGithub
                            "codehub" -> faCopyright
                            else -> faSignInAlt
                        }
                    )
                }
            }
        }
    }
}

private fun ChildrenBuilder.oauthLogin(provider: OauthProviderInfo, icon: dynamic) {
    div {
        className = ClassName("col text-center px-1")
        a {
            href = provider.authorizationLink
            className = ClassName("btn btn-link px-3 text-white text-lg text-center")
            style = jso {
                fontSize = "3.2rem".unsafeCast<FontSize>()
            }
            fontAwesomeIcon(icon = icon)
        }
    }
}
