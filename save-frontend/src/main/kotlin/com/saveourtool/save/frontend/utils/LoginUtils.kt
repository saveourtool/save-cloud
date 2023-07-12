package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.info.OauthProviderInfo
import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import web.cssom.*

/**
 * @param size font size of oauth logos
 * @param provider oauth provider (Huawei, Gitee, Github, etc.)
 * @param icon icon logo
 * @param animate
 * @param label
 * @return
 */
fun ChildrenBuilder.processRegistrationId(
    size: FontSize,
    provider: OauthProviderInfo,
    animate: String,
    label: String = ""
) = oauthLoginForKnownAwesomeIcons(
    size,
    provider,
    animate,
    label,
    mapUnknownIcons(provider.registrationId),
    mapKnownIcons(provider.registrationId)
)

/**
 * @param size font size of oauth logos
 * @param provider oauth provider (Huawei, Gitee, Github, etc.)
 * @param icon icon logo
 * @param animate
 * @param label
 */
private fun ChildrenBuilder.oauthLoginForKnownAwesomeIcons(
    size: FontSize,
    provider: OauthProviderInfo,
    animate: String,
    label: String = "",
    staticSvg: String = "",
    icon: dynamic
) {
    div {
        className = ClassName("animated-provider col animate__animated $animate")
        a {
            href = provider.authorizationLink
            className = ClassName("text-center")
            div {
                className = ClassName("col text-center")
                style = jso {
                    fontSize = size
                }
                // if there is no fontAwesome for this brand we use simple static SVG
                if (staticSvg.isEmpty()) {
                    fontAwesomeIcon(icon)
                } else {
                    img {
                        src = staticSvg
                        style = jso {
                            width = size.unsafeCast<Width>()
                            height = size.unsafeCast<Height>()
                        }
                    }
                }
            }
            div {
                className = ClassName("col text text-center")
                +label
            }
        }
    }
}

/**
 * @param registrationId oauth provider name (same as in spring security config) from api-gateway
 */
fun mapKnownIcons(registrationId: String) =
        when (registrationId) {
            "github" -> faGithub
            "google" -> faGoogle
            "codehub" -> faCopyright
            else -> faSignInAlt
        }

/**
 * @param registrationId
 */
fun mapUnknownIcons(registrationId: String) =
        when (registrationId) {
            "huawei" -> "img/huawei.svg"
            "gitee" -> "img/gitee.svg"
            else -> ""
        }
