/**
 * rendering for Email and Login management card
 */

package com.saveourtool.save.frontend.components.views.usersettings.right

import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.views.usersettings.SettingsProps
import com.saveourtool.save.frontend.components.views.usersettings.inputForm
import com.saveourtool.save.frontend.components.views.usersettings.saveUser
import com.saveourtool.save.frontend.utils.*
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.useState
import web.cssom.ClassName

val email: FC<SettingsProps> = FC { props ->
    val (fieldsMap, setFieldsMap) =
            useState<MutableMap<InputTypes, String?>>(mutableMapOf())
    val (fieldsValidationMap, setfieldsValidationMap) =
            useState<MutableMap<InputTypes, String?>>(mutableMapOf())

    val saveUser = saveUser(fieldsMap, props, fieldsValidationMap, setfieldsValidationMap)

    div {
        className = ClassName("col mt-2 px-4")
        inputForm(props.userInfo?.name, InputTypes.USER_NAME, fieldsMap, fieldsValidationMap, setFieldsMap)
        inputForm(props.userInfo?.email, InputTypes.USER_EMAIL, fieldsMap, fieldsValidationMap, setFieldsMap)

        @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
        hr { }
        div {
            className = ClassName("row justify-content-center")
            buttonBuilder("Save changes", style = "primary") {
                saveUser()
            }
        }
    }
}
