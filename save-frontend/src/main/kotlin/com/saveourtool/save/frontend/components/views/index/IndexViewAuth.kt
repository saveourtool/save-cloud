/**
 * Authorization component (Oauth2 elements) for Index View
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.info.OauthProviderInfo

import org.w3c.fetch.Headers
import react.FC
import react.VFC
import react.dom.html.ReactHTML.div
import react.useState
import web.cssom.ClassName
import web.cssom.rem

import kotlinx.browser.window

val indexAuth: FC<IndexViewProps> = FC { _ ->
    val (oauthProviders, setOauthProviders) = useState(emptyList<OauthProviderInfo>())
    val getOauthProviders = useDeferredRequest {
        val availableProviders: List<OauthProviderInfo> = get(
            "${window.location.origin}/sec/oauth-providers",
            Headers(),
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        ).run {
            if (ok) decodeFromJsonString() else emptyList()
        }

        setOauthProviders(availableProviders)
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
            div {
                className = ClassName("row")
                oauthProviders.sortedBy {
                    it.registrationId
                }.map { userInfo ->
                    val oauthProvider = userInfo.registrationId
                    processRegistrationId(
                        OauthProvidersFeConfig(
                            3.5.rem,
                            userInfo,
                            "animate__backInUp",
                            oauthProvider.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
                        )
                    )
                }
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
            className = ClassName("col-6 mt-2")

            div {
                className = ClassName("separator text-black")
                +"Sign in with"
            }
        }
        div {
            className = ClassName("col-3")
        }
    }
}
