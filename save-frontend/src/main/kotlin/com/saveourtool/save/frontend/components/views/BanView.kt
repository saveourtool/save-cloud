/**
 * View for Ban
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.frontend.utils.Style
import com.saveourtool.save.frontend.utils.useBackground
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.info.UserStatus
import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import react.router.useNavigate
import web.cssom.ClassName
import web.cssom.rem

private const val BAN = """
    BAN
"""

private const val SUPPORT = """
    You have been banned from our platform. If you believe this was a mistake, please contact our support team at
"""

val banView: FC<BanProps> = FC { props ->
    useBackground(Style.SAVE_LIGHT)

    val navigate = useNavigate()
    if (props.userInfo?.status != UserStatus.BANNED) {
        navigate("/", jso { replace = true })
    }

    div {
        className = ClassName("col text-center")
        style = jso {
            height = 40.rem
        }

        div {
            className = ClassName("col error mx-auto mt-5")
            asDynamic()["data-text"] = BAN
            +BAN
        }

        p {
            className = ClassName("lead text-gray-800 mb-3")
            +SUPPORT
            a {
                href = "mailto:saveourtool@gmail.com"
                strong {
                    className = ClassName("d-inline-block mb-2")
                    +" saveourtool@gmail.com."
                }
            }
        }
    }
}

/**
 * `Props` retrieved from router
 */
external interface BanProps : Props {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?
}
