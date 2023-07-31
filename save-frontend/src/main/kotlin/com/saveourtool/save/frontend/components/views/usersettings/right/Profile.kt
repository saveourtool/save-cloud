package com.saveourtool.save.frontend.components.views.usersettings.right

import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.views.usersettings.SettingsProps
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.frontend.utils.useWindowOpenness
import react.ChildrenBuilder
import react.FC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import web.cssom.ClassName
import web.html.InputType

val profile = FC<SettingsProps> { props ->
    val card = cardComponent(isBordered = false, hasBg = true)

    // val deleteUserWindowOpenness = useWindowOpenness()
    card {
        ReactHTML.div {
            className = ClassName("col mt-2 mx-2")
            inputForm(props.userInfo?.name, "User name:")
            inputForm(props.userInfo?.company, "Company/affiliation:")
            inputForm(props.userInfo?.location, "Location:")
            inputForm(props.userInfo?.linkedin, "LinkedIn:")
            inputForm(props.userInfo?.gitHub, "GitHub:")
            inputForm(props.userInfo?.twitter, "Twitter/X:")

            /*            state.conflictErrorMessage?.let {
                ReactHTML.div {
                    className = ClassName("invalid-feedback d-block")
                    +it
                }
            }*/

            ReactHTML.hr {}
            ReactHTML.div {
                className = ClassName("row d-flex justify-content-center")
                ReactHTML.div {
                    className = ClassName("col-8 d-sm-flex align-items-center justify-content-center")

                    ReactHTML.div {
                        className = ClassName("col-4")
                        buttonBuilder("Save changes", style = "primary", classes = "mr-3") {
/*
                        updateUser()
*/
                        }
                    }
                }
            }
        }
    }
}

private fun ChildrenBuilder.inputForm(previousValue: String?, title: String) {
    div {
        className = ClassName("row")
        div {
            className = ClassName("col-5 mt-2 text-left align-self-center")
            +title
        }
        div {
            className = ClassName("col-7 mt-2 input-group pl-0")
            input {
                type = InputType.text
                className = ClassName("form-control")
                previousValue.let {
                    defaultValue = it
                }
                onChange = {
                    // changeFields(InputTypes.TWITTER, it)
                }
            }
        }
    }
}
