package com.saveourtool.save.frontend.components.views.usersettingsview

import com.saveourtool.save.frontend.components.basic.InputTypes
import com.saveourtool.save.frontend.components.basic.cardComponent
import csstype.ClassName

import react.FC
import react.dom.*

import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input

@Suppress("MISSING_KDOC_TOP_LEVEL")
class UserSettingsProfileMenuView : UserSettingsView() {
    private val card = cardComponent(isBordered = false, hasBg = true)
    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "EMPTY_BLOCK_STRUCTURE_ERROR")
    override fun renderMenu(): FC<UserSettingsProps> = FC { props ->
        card {
            div {
                className = ClassName("row mt-2 ml-2 mr-2")
                div {
                    className = ClassName("col-5 mt-2 text-left align-self-center")
                    +"Company:"
                }
                div {
                    className = ClassName("col-7 mt-2 input-group pl-0")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        state.userInfo?.company?.let {
                            defaultValue = it
                        }
                        onChange = {
                            changeFields(InputTypes.COMPANY, it)
                        }
                    }
                }

                div {
                    className = ClassName("col-5 mt-2 text-left align-self-center")
                    +"Location:"
                }
                div {
                    className = ClassName("col-7 mt-2 input-group pl-0")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        state.userInfo?.location?.let {
                            defaultValue = it
                        }
                        onChange = {
                            changeFields(InputTypes.LOCATION, it)
                        }
                    }
                }

                div {
                    className = ClassName("col-5 mt-2 text-left align-self-center")
                    +"Linkedin:"
                }
                div {
                    className = ClassName("col-7 mt-2 input-group pl-0")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        state.userInfo?.linkedin?.let {
                            defaultValue = it
                        }
                        onChange = {
                            changeFields(InputTypes.LINKEDIN, it)
                        }
                    }
                }

                div {
                    className = ClassName("col-5 mt-2 text-left align-self-center")
                    +"GitHub:"
                }
                div {
                    className = ClassName("col-7 mt-2 input-group pl-0")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        state.userInfo?.gitHub?.let {
                            defaultValue = it
                        }
                        onChange = {
                            changeFields(InputTypes.GIT_HUB, it)
                        }
                    }
                }

                div {
                    className = ClassName("col-5 mt-2 text-left align-self-center")
                    +"Twitter:"
                }
                div {
                    className = ClassName("col-7 mt-2 input-group pl-0")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        state.userInfo?.twitter?.let {
                            defaultValue = it
                        }
                        onChange = {
                            changeFields(InputTypes.TWITTER, it)
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
