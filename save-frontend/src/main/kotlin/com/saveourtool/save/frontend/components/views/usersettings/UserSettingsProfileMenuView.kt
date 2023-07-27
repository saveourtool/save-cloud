package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.utils.*

import react.VFC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input
import web.cssom.ClassName
import web.html.InputType

@Suppress("MISSING_KDOC_TOP_LEVEL")
class UserSettingsProfileMenuView : UserSettingsView() {
    private val card = cardComponent(isBordered = false, hasBg = true)
    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "EMPTY_BLOCK_STRUCTURE_ERROR")
    override fun renderMenu(): VFC = VFC {
        val deleteUserWindowOpenness = useWindowOpenness()

        displayModal(
            deleteUserWindowOpenness.isOpen(),
            "Deletion of user profile",
            "Are you sure you want to permanently delete your profile? You will never be able to restore it again.",
            mediumTransparentModalStyle,
            deleteUserWindowOpenness.closeWindowAction(),
        ) {
            buttonBuilder("Yes") {
                deleteUser()
                deleteUserWindowOpenness.closeWindow()
            }
            buttonBuilder("Cancel", "secondary") {
                deleteUserWindowOpenness.closeWindow()
            }
        }

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
                    +"Company/affiliation:"
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
                    className = ClassName("col-8 d-sm-flex align-items-center justify-content-center")

                    div {
                        className = ClassName("col-4")
                        buttonBuilder("Save changes", style = "primary", classes = "mr-3") {
                            updateUser()
                        }
                    }

                    div {
                        className = ClassName("col-4")
                        buttonBuilder("Delete your profile", style = "danger") {
                            deleteUserWindowOpenness.openWindow()
                        }
                    }
                }
            }
        }
    }
}
