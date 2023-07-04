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
import react.VFC
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
        className = ClassName("row mt-2")
        div {
            className = ClassName("col-3 text-center")
        }
        div {
            className = ClassName("col-6 text-center")
            @Suppress("MAGIC_NUMBER")
            oauthProviders.map {
                oauthLogin(
                    5.rem, it, "animate__backInUp",
                    when (it.registrationId) {
                        "github" -> faGithub
                        "codehub" -> faCopyright
                        else -> faSignInAlt
                    }
                )
            }
        }
        div {
            className = ClassName("col-3 text-center")
        }
    }
}

val separator = VFC {
    div {
        className = ClassName("row")
        div {
            className = ClassName("col-3")
        }

        div {
            className = ClassName("col-6")

            div {
                className = ClassName("separator mt-5 text-white")
                +"Sign in"
            }
        }
        div {
            className = ClassName("col-3")
        }
    }
}

/**
 * @param size font size of oauth logos
 * @param provider oauth provider (Huawei, Gitee, Github, etc.)
 * @param icon icon logo
 * @param animate
 */
fun ChildrenBuilder.oauthLogin(
    size: FontSize,
    provider: OauthProviderInfo,
    animate: String,
    icon: dynamic
) {
    a {
        href = provider.authorizationLink
        className = ClassName("btn btn-link px-5 text-white text-lg text-center animate__animated $animate")
        style = jso {
            fontSize = size
        }
        fontAwesomeIcon(icon = icon)
    }
}
