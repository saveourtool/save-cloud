package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.views.usersettings.right.profile
import com.saveourtool.save.validation.FrontendRoutes
import react.FC

val rightColumn = FC<SettingsProps> { props ->
    when(props.type) {
        FrontendRoutes.SETTINGS_PROFILE -> profile {
            this.userInfo = props.userInfo
            this.type = props.type
        }
        else -> {

        }
    }
}


