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

val emailSettingsCard: FC<SettingsProps> = FC { props ->
    val (settingsInputFields, setSettingsInputFields) = useState(SettingsInputFields())
    val saveUser = saveUser(props, settingsInputFields, setSettingsInputFields)

    div {
        className = ClassName("col mt-2 px-4")
        inputForm(props.userInfo?.name, InputTypes.USER_NAME, settingsInputFields, setSettingsInputFields)
        inputForm(props.userInfo?.email, InputTypes.USER_EMAIL, settingsInputFields, setSettingsInputFields)

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
