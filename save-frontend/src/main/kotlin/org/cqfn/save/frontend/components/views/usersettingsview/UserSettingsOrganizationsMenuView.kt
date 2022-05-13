package org.cqfn.save.frontend.components.views.usersettingsview

import org.cqfn.save.frontend.components.basic.cardComponent

import kotlinext.js.assign
import org.w3c.fetch.Headers
import react.FC
import react.dom.*
import react.fc
import react.setState

import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import org.cqfn.save.frontend.utils.*
import org.cqfn.save.info.UserInfo
import react.useState

@Suppress("MISSING_KDOC_TOP_LEVEL")
class UserSettingsOrganizationsMenuView : UserSettingsView() {
    override fun renderMenu(): FC<UserSettingsProps> = fc { props ->
        child(cardComponent(isBordered = false, hasBg = true) {
            div("d-sm-flex align-items-center justify-content-center mb-4") {
                h1("h3 mb-0 mt-2 text-gray-800") {
                    +"Organizations"
                }
            }
            val organizationsAndRoles = state.userInfo?.organizations ?: emptyMap()

            for ((organization, role) in organizationsAndRoles) {
                div("row justify-content-center") {
                    a(href = "#/$organization") {
                        +organization
                    }
                    div {
                        +role.formattedName
                    }
                }
            }
        })
    }
}
