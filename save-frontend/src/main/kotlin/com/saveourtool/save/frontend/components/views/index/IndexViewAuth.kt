package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.externals.fontawesome.faCopyright
import com.saveourtool.save.frontend.externals.fontawesome.faGithub
import com.saveourtool.save.frontend.externals.fontawesome.faSignInAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.info.OauthProviderInfo
import com.saveourtool.save.info.UserInfo
import js.core.jso
import kotlinx.browser.window
import org.w3c.fetch.Headers
import react.ChildrenBuilder
import react.FC
import react.VFC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.useState
import web.cssom.BackgroundColor
import web.cssom.ClassName
import web.cssom.FontSize

val indexAuth: FC<IndexViewProps> = FC { props ->
/*    val (oauthProviders, setOauthProviders) = useState(emptyList<OauthProviderInfo>())

    val getOauthProviders = useDeferredRequest {
        val oauthProviderInfoList: List<OauthProviderInfo>? = get(
            "${window.location.origin}/sec/oauth-providers",
            Headers(),
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        ).run {
            if (ok) decodeFromJsonString() else null
        }

        println(oauthProviderInfoList)

        oauthProviderInfoList?.let {
            setOauthProviders(oauthProviders)
        }
    }*/

    val (oauthProviders, setOauthProviders) = useState(emptyList<OauthProviderInfo>())
    val getOauthProviders = useDeferredRequest {
        val usersFromDb: List<OauthProviderInfo> = get(
            "${window.location.origin}/sec/oauth-providers",
            Headers(),
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        ).run {
            if (ok) decodeFromJsonString() else emptyList()
        }

        setOauthProviders(usersFromDb)
    }

    useOnce {
        getOauthProviders()
    }

    div {
        className = ClassName("row mt-5")
        div {
            className = ClassName("col text-center mt-5")
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

private fun ChildrenBuilder.oauthLogin(provider: OauthProviderInfo, icon: dynamic) {
    a {
        href = provider.authorizationLink
        className = ClassName("btn btn-link px-3 text-white text-lg text-center")
        style = jso {
            fontSize = "3.2rem".unsafeCast<FontSize>()
        }
        fontAwesomeIcon(icon = icon)
    }
}
