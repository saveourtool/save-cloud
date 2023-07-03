/**
 * Authorization component (Oauth2 elements) for Index View
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.externals.fontawesome.faCopyright
import com.saveourtool.save.frontend.externals.fontawesome.faGithub
import com.saveourtool.save.frontend.externals.fontawesome.faSignInAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.info.OauthProviderInfo

import js.core.jso
import org.w3c.fetch.Headers
import react.ChildrenBuilder
import react.FC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.useState
import web.cssom.ClassName
import web.cssom.FontSize
import web.cssom.rem

import kotlinx.browser.window

val indexAuth: FC<IndexViewProps> = FC { props ->
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
                    5.rem,
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

/**
 * @param size font size of oauth logos
 * @param provider oauth provider (Huawei, Gitee, Github, etc.)
 * @param icon icon logo
 */
fun ChildrenBuilder.oauthLogin(size: FontSize, provider: OauthProviderInfo, icon: dynamic) {
    a {
        href = provider.authorizationLink
        className = ClassName("btn btn-link px-5 text-white text-lg text-center")
        style = jso {
            fontSize = size
        }
        fontAwesomeIcon(icon = icon)
    }
}
