package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.inputform.InputTypes

import react.VFC
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input
import web.cssom.ClassName
import web.html.ButtonType
import web.html.InputType

@Suppress("MISSING_KDOC_TOP_LEVEL")
class UserSettingsProfileMenuView : UserSettingsView() {
    private val card = cardComponent(isBordered = false, hasBg = true)
    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "EMPTY_BLOCK_STRUCTURE_ERROR")
    override fun renderMenu(): VFC = VFC {
        card {
            div {
                className = ClassName("row mt-2 ml-2 mr-2")
                div {
                    className = ClassName("col-5 mt-2 text-left align-self-center")
                    +"User name:"
                }
                div {
                    className = ClassName("col-7 mt-2 input-group pl-0")
                    input {
                        type = InputType.text
                        className = ClassName("form-control")
                        state.userInfo?.name?.let {
                            defaultValue = it
                        }
                        onChange = {
                            changeFields(InputTypes.USER_NAME, it)
                        }
                    }
                }
                state.conflictErrorMessage?.let {
                    div {
                        className = ClassName("invalid-feedback d-block")
                        +it
                    }
                }

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
                        className = ClassName("btn btn-sm btn-outline-primary")
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
