package org.cqfn.save.frontend.components.views.usersettingsview

import org.cqfn.save.frontend.components.basic.cardComponent

import react.FC
import react.dom.*
import react.fc
import react.setState

import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction

@Suppress("MISSING_KDOC_TOP_LEVEL")
class UserSettingsTokenMenuView : UserSettingsView() {
    override fun renderMenu(): FC<UserSettingsProps> = fc { props ->
        child(cardComponent(isBordered = false, hasBg = true) {
            div("d-sm-flex align-items-center justify-content-center mb-4") {
                h1("h3 mb-0 mt-2 text-gray-800") {
                    +"Personal access tokens"
                }
            }

            div("row justify-content-center") {
                button(type = ButtonType.button, classes = "btn btn-primary mb-2 mr-2") {
                    +"Generate new token"
                    attrs.onClickFunction = {
                        generateToken()
                    }
                }
            }

            state.token?.let {
                div("col-md-12 mt-3") {
                    input {
                        attrs.value = state.token ?: ""
                        attrs["required"] = true
                        attrs["class"] = "form-control"
                    }
                    div("invalid-feedback d-block") {
                        +"This is your unique token. It will be shown to you only once. Please remember it."
                    }
                }
            }
        })
    }

    @Suppress("MAGIC_NUMBER")
    private fun generateToken() {
        var token = "ghp_"
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        while (token.length < 40) {
            token += charPool.random()
        }
        setState {
            this.token = token
        }
        updateUser()
    }
}
