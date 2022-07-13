package com.saveourtool.save.frontend.components.views.usersettingsview

import com.saveourtool.save.frontend.components.basic.InputTypes
import com.saveourtool.save.frontend.components.basic.cardComponent
import csstype.ClassName

import react.FC

import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input

@Suppress("MISSING_KDOC_TOP_LEVEL")
class UserSettingsEmailMenuView : UserSettingsView() {
    private val emailCard = cardComponent(isBordered = false, hasBg = true)
    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION")
    override fun renderMenu(): FC<UserSettingsProps> = FC { props ->
        emailCard {
            div {
                className = ClassName("row mt-2 ml-2 mr-2")
                div {
                    className = ClassName("col-5 text-left align-self-center")
                    +"User email:"
                }
                div {
                    className = ClassName("col-7 input-group pl-0")
                    input {
                        type = InputType.email
                        className = ClassName("form-control")
                        state.userInfo?.email?.let {
                            defaultValue = it
                        }
                        placeholder = "email@example.com"
                        onChange = {
                            changeFields(InputTypes.USER_EMAIL, it)
                        }
                    }
                }
            }

            hr {}
            div {
                className = ClassName("row d-flex justify-content-center")
                div {
                    className = ClassName("col-3 d-sm-flex align-items-center justify-content-center")
                    button {
                        type = ButtonType.button
                        className = ClassName("btn btn-sm btn-primary")
                        onClick = {
                            updateUser()
                        }
                        +"Save changes"
                    }
                }
            }
        }
    }
}
