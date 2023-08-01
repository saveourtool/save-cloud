package com.saveourtool.save.frontend.components.views.usersettings.right

import com.saveourtool.save.frontend.components.views.usersettings.SettingsProps
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import js.core.jso
import react.FC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.p
import react.useState
import web.cssom.ClassName
import web.html.ButtonType
import web.cssom.rem
import web.cssom.TextAlign
import react.dom.html.ReactHTML.a
import com.saveourtool.save.frontend.utils.buttonBuilder


// FixMe: add info about last created token

const val TOKEN_TEXT = """
    This Token serves as an integral part of establishing a secure and stable connection 
    between your automated API and the SAVE platform. 
    The purpose of this Token is to provide capability to connect to SAVE Public API from different CI services: 
    to upload benchmarks, get results, etc. Can be extremely useful for your automated CI and testing process.
"""

val token = FC<SettingsProps> { props ->
    val (token, setToken) = useState<String>()
    div {
        className = ClassName("row justify-content-center mt-5")
        img {
            src = "/img/settings_icon2.png"
            style = jso {
                height = 10.rem
                width = 10.rem
            }
        }
    }

    div {
        className = ClassName("row justify-content-center mt-4")
        ReactHTML.h2 {
            className = ClassName("text-gray-800")
            +"Personal access token"
        }
    }

    div {
        className = ClassName("row justify-content-center mt-4")
        div {
            className = ClassName("col-6")
            p {
                style = jso {
                    textAlign = TextAlign.justify
                }
                +TOKEN_TEXT
            }
        }
    }

    div {
        className = ClassName("row justify-content-center mt-4")
        div {
            className = ClassName("col-6")
            ReactHTML.h4 {
                className = ClassName("text-gray-800 text-center")
                +"Read more:"
            }
        }
    }

    div {
        className = ClassName("row justify-content-center mt-4")
        div {
            className = ClassName("col-1 text-center")
            a {
                href = "https://github.com/saveourtool/save-cloud/tree/master/save-api"
                buttonBuilder(
                    "Readme",
                    style = "info rounded-pill",
                    isOutline = false
                ) {

                }
            }
        }
        div {
            className = ClassName("col-1 text-center")
            a {
                href = "https://github.com/saveourtool/save-cloud/tree/master/save-api-cli"
                buttonBuilder(
                    "Example",
                    style = "info rounded-pill",
                    isOutline = false
                ) {

                }
            }
        }
    }


    div {
        className = ClassName("row justify-content-center mt-4")
        ReactHTML.button {
            type = ButtonType.button
            className = ClassName("btn btn-outline-primary")
            +"Generate new token"
            onClick = {
                setToken(generateToken())
                postToken(props.userInfo!!, token)
            }
        }
    }

    token?.let {
        div {
            className = ClassName("row justify-content-center mt-4")
            div {
                className = ClassName("col-6")
                form {
                    className = ClassName("form-group text-center")
                    input {
                        value = token
                        required = true
                        className = ClassName("form-control text-center")
                    }
                }
                div {
                    className = ClassName("invalid-feedback d-block text-center")
                    +"This is your unique token. It will be shown to you only once! Please save it."
                }
            }
        }
    }
}

@Suppress("MAGIC_NUMBER")
private fun generateToken(): String {
    var token = "ghp_"
    val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    while (token.length < 40) {
        token += charPool.random()
    }
    return token
}

private fun postToken(userInfo: UserInfo, token: String?) {
    useDeferredRequest {
        post(
            "$apiUrl/users/${userInfo.name}/save/token",
            jsonHeaders,
            token,
            loadingHandler = ::loadingHandler,
        )
    }
}
