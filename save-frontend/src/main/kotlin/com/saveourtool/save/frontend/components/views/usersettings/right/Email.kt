/**
 * rendering for Email and Login management card
 */

package com.saveourtool.save.frontend.components.views.usersettings.right

import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.views.usersettings.SettingsProps
import com.saveourtool.save.frontend.components.views.usersettings.inputForm
import com.saveourtool.save.frontend.components.views.usersettings.useSaveUser
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.FC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.hr
import react.router.dom.Link
import react.useState
import web.cssom.ClassName
import web.cssom.rem

val emailSettingsCard: FC<SettingsProps> = FC { props ->
    val (settingsInputFields, setSettingsInputFields) = useState(SettingsInputFields())
    val saveUser = useSaveUser(props, settingsInputFields, setSettingsInputFields)

    div {
        className = ClassName("row justify-content-center mt-5")
        ReactHTML.img {
            src = "/img/settings_icon1.png"
            style = jso {
                height = 10.rem
                width = 10.rem
            }
        }
    }

    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mb-4 mt-4")
        h3 {
            className = ClassName("mt-2 mr-2 text-gray-800")
            +"Login and Email"
        }

        Link {
            to = "/${FrontendRoutes.TERMS_OF_USE}"
            buttonBuilder(
                "terms of usage",
                style = "outline-secondary rounded-pill btn-sm",
                isOutline = false
            ) {
            }
        }
    }

    div {
        className = ClassName("col mt-2 px-5")
        inputForm(props.userInfo?.name, InputTypes.USER_NAME, settingsInputFields, setSettingsInputFields, colRatio = "col-2" to "col-6")
        inputForm(props.userInfo?.email, InputTypes.USER_EMAIL, settingsInputFields, setSettingsInputFields, colRatio = "col-2" to "col-6")

        div {
            className = ClassName("row justify-content-center")
            div {
                className = ClassName("col-8")
                @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
                hr { }
            }
        }

        div {
            className = ClassName("row justify-content-center")
            buttonBuilder("Save changes", style = "primary") {
                saveUser()
            }
        }
    }
}
