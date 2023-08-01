package com.saveourtool.save.frontend.components.views.usersettings.right

import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.views.usersettings.SettingsProps
import com.saveourtool.save.frontend.components.views.usersettings.createNewUser
import com.saveourtool.save.frontend.components.views.usersettings.inputForm
import com.saveourtool.save.frontend.components.views.usersettings.saveUser
import com.saveourtool.save.frontend.utils.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.Headers
import react.ChildrenBuilder
import react.FC
import react.StateSetter
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input
import react.useState
import web.cssom.ClassName
import web.html.InputType

val email = FC<SettingsProps> { props ->
    val (fieldsMap, setFieldsMap) =
        useState<MutableMap<InputTypes, String>>(mutableMapOf())
    val (validationToolTips, setValidationToolTips) =
        useState<MutableMap<InputTypes, String?>>(mutableMapOf())

    val saveUser = saveUser(fieldsMap, props, validationToolTips, setValidationToolTips)

    div {
        className = ClassName("col mt-2 px-4")
        inputForm(props.userInfo?.name, InputTypes.USER_NAME, fieldsMap, validationToolTips, setFieldsMap)
        inputForm(props.userInfo?.email, InputTypes.USER_EMAIL, fieldsMap, validationToolTips, setFieldsMap)

        hr { }
        div {
            className = ClassName("row justify-content-center")
            buttonBuilder("Save changes", style = "primary") {
                saveUser()
            }
        }
    }
}
