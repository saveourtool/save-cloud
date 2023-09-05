/**
 * View for users that are being reviewed until approve status gotten
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.frontend.externals.i18next.useTranslation
import com.saveourtool.save.frontend.utils.Style
import com.saveourtool.save.frontend.utils.useBackground
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.info.UserStatus
import js.core.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import react.router.useNavigate
import web.cssom.ClassName
import web.cssom.rem

val thanksForRegistrationView: FC<BanProps> = FC { props ->
    useBackground(Style.INDEX)

    val navigate = useNavigate()
    val (t) = useTranslation("thanks-for-registration")
    if (props.userInfo?.status != UserStatus.NOT_APPROVED) {
        navigate("/", jso { replace = true })
    }

    div {
        className = ClassName("col text-center d-flex justify-content-center align-items-center")
        style = jso {
            height = 52.rem
        }

        div {
            className = ClassName("col-5 bg-light mb-5 p-3 rounded")
            img {
                className = ClassName("avatar avatar-user border color-bg-default rounded-circle")
                style = jso {
                    width = 20.rem
                }
                src = "/img/avatar_packs/avatar1.png"
            }

            div {
                className = ClassName("p-2")
                h2 {
                    className = ClassName("mx-auto")
                    +"${"Thank you for registration".t()}!"
                }

                p {
                    className = ClassName("lead text-gray-800 mb-0")
                    +"Your request is pending review".t()
                    a {
                        className = ClassName("ml-1")
                        href = "mailto:saveourtool@gmail.com"
                        strong {
                            className = ClassName("d-inline-block")
                            +"  saveourtool@gmail.com."
                        }
                    }
                }
            }
        }
    }
}

/**
 * `Props` retrieved from router
 */
external interface ThanksForRegistrationViewProps : Props {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?
}
