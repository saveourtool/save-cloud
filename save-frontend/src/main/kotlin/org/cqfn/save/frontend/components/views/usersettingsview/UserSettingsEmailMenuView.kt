package com.saveourtool.save.frontend.components.views.usersettingsview

import com.saveourtool.save.frontend.components.basic.InputTypes
import com.saveourtool.save.frontend.components.basic.cardComponent

import react.FC
import react.dom.*
import react.fc

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction

@Suppress("MISSING_KDOC_TOP_LEVEL")
class UserSettingsEmailMenuView : UserSettingsView() {
    override fun renderMenu(): FC<UserSettingsProps> = fc { props ->
        child(cardComponent(isBordered = false, hasBg = true) {
            div("row mt-2 ml-2 mr-2") {
                div("col-5 text-left align-self-center") {
                    +"User email:"
                }
                div("col-7 input-group pl-0") {
                    input(type = InputType.email) {
                        attrs["class"] = "form-control"
                        attrs {
                            state.userInfo?.email?.let {
                                defaultValue = it
                            }
                            placeholder = "email@example.com"
                            onChangeFunction = {
                                changeFields(InputTypes.USER_EMAIL, it)
                            }
                        }
                    }
                }
            }

            hr("") {}
            div("row d-flex justify-content-center") {
                div("col-3 d-sm-flex align-items-center justify-content-center") {
                    button(type = ButtonType.button, classes = "btn btn-sm btn-primary") {
                        attrs.onClickFunction = {
                            updateUser()
                        }
                        +"Save changes"
                    }
                }
            }
        })
    }
}
