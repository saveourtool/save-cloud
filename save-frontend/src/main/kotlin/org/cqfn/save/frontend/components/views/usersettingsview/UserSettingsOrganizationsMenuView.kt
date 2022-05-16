package org.cqfn.save.frontend.components.views.usersettingsview

import org.cqfn.save.domain.Role
import org.cqfn.save.frontend.components.basic.cardComponent
import org.cqfn.save.v1

import react.FC
import react.dom.*
import react.fc

@Suppress("MISSING_KDOC_TOP_LEVEL", "TOO_LONG_FUNCTION")
class UserSettingsOrganizationsMenuView : UserSettingsView() {
    override fun renderMenu(): FC<UserSettingsProps> = fc { props ->
        child(cardComponent(isBordered = false, hasBg = true) {
            div("d-sm-flex align-items-center justify-content-center mb-4 mt-4") {
                h1("h3 mb-0 mt-2 text-gray-800") {
                    +"Organizations"
                }
            }

            ul(classes = "list-group list-group-flush") {
                for (organizationInfo in state.selfOrganizationInfos) {
                    li("list-group-item") {
                        div("row justify-content-between align-items-center") {
                            div("align-items-center ml-3") {
                                img(classes = "avatar avatar-user width-full border color-bg-default rounded-circle") {
                                    attrs.src = organizationInfo.avatar?.let {
                                        "/api/$v1/avatar$it"
                                    } ?: "img/company.svg"
                                    attrs.height = "60"
                                    attrs.width = "60"
                                }
                                a(classes = "ml-2", href = "#/${organizationInfo.name}") {
                                    +organizationInfo.name
                                }
                            }
                            div("mr-3") {
                                val role = state.userInfo?.name?.let {
                                    organizationInfo.userRoles[it]
                                } ?: Role.NONE
                                +role.formattedName
                            }
                        }
                    }
                }
            }
        })
    }
}
