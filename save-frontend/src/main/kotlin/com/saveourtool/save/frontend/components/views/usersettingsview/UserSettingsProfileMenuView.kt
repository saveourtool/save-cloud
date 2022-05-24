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
class UserSettingsProfileMenuView : UserSettingsView() {
    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    override fun renderMenu(): FC<UserSettingsProps> = fc { props ->
        child(cardComponent(isBordered = false, hasBg = true) {
            div("row mt-2 ml-2 mr-2") {
                div("col-5 mt-2 text-left align-self-center") {
                    +"Company:"
                }
                div("col-7 mt-2 input-group pl-0") {
                    input(type = InputType.text) {
                        attrs["class"] = "form-control"
                        attrs {
                            state.userInfo?.company?.let {
                                defaultValue = it
                            }
                            onChangeFunction = {
                                changeFields(InputTypes.COMPANY, it)
                            }
                        }
                    }
                }

                div("col-5 mt-2 text-left align-self-center") {
                    +"Location:"
                }
                div("col-7 mt-2 input-group pl-0") {
                    input(type = InputType.text) {
                        attrs["class"] = "form-control"
                        attrs {
                            state.userInfo?.location?.let {
                                defaultValue = it
                            }
                            onChangeFunction = {
                                changeFields(InputTypes.LOCATION, it)
                            }
                        }
                    }
                }

                div("col-5 mt-2 text-left align-self-center") {
                    +"Linkedin:"
                }
                div("col-7 mt-2 input-group pl-0") {
                    input(type = InputType.text) {
                        attrs["class"] = "form-control"
                        attrs {
                            state.userInfo?.linkedin?.let {
                                defaultValue = it
                            }
                            onChangeFunction = {
                                changeFields(InputTypes.LINKEDIN, it)
                            }
                        }
                    }
                }

                div("col-5 mt-2 text-left align-self-center") {
                    +"GitHub:"
                }
                div("col-7 mt-2 input-group pl-0") {
                    input(type = InputType.text) {
                        attrs["class"] = "form-control"
                        attrs {
                            state.userInfo?.gitHub?.let {
                                defaultValue = it
                            }
                            onChangeFunction = {
                                changeFields(InputTypes.GIT_HUB, it)
                            }
                        }
                    }
                }

                div("col-5 mt-2 text-left align-self-center") {
                    +"Twitter:"
                }
                div("col-7 mt-2 input-group pl-0") {
                    input(type = InputType.text) {
                        attrs["class"] = "form-control"
                        attrs {
                            state.userInfo?.twitter?.let {
                                defaultValue = it
                            }
                            onChangeFunction = {
                                changeFields(InputTypes.TWITTER, it)
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
