package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.views.usersettings.right.profile
import com.saveourtool.save.validation.FrontendRoutes

import react.FC
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

val rightColumn = FC<SettingsProps> { props ->
    // val deleteUserWindowOpenness = useWindowOpenness()
    div {
        className = ClassName("card card-body mt-0 pt-0 px-0 text-gray-800")
        style = cardHeight
        when (props.type) {
            FrontendRoutes.SETTINGS_PROFILE -> profile {
                this.userInfo = props.userInfo
                this.type = props.type
            }

            else -> {

            }
        }
    }
}
