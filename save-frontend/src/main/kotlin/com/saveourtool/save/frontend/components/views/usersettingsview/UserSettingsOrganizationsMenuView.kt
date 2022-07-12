package com.saveourtool.save.frontend.components.views.usersettingsview

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.v1
import csstype.ClassName

import react.FC
import react.dom.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul

@Suppress("MISSING_KDOC_TOP_LEVEL", "TOO_LONG_FUNCTION")
class UserSettingsOrganizationsMenuView : UserSettingsView() {
    private val organizationListCard = cardComponent(isBordered = false, hasBg = true)
    override fun renderMenu(): FC<UserSettingsProps> = FC { props ->
        organizationListCard {
            div {
                className = ClassName("d-sm-flex align-items-center justify-content-center mb-4 mt-4")
                h1 {
                    className = ClassName("h3 mb-0 mt-2 text-gray-800")
                    +"Organizations"
                }
            }

            ul {
                className = ClassName("list-group list-group-flush")
                for (organizationInfo in state.selfOrganizationInfos) {
                    li {
                        className = ClassName("list-group-item")
                        div {
                            className = ClassName("row justify-content-between align-items-center")
                            div {
                                className = ClassName("align-items-center ml-3")
                                img {
                                    className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                    src = organizationInfo.avatar?.let {
                                        "/api/$v1/avatar$it"
                                    } ?: "img/company.svg"
                                    height = 60.0
                                    width = 60.0
                                }
                                a {
                                    className = ClassName("ml-2")
                                    href = "#/${organizationInfo.name}"
                                    +organizationInfo.name
                                }
                            }
                            div {
                                className = ClassName("mr-3")
                                val role = state.userInfo?.name?.let {
                                    organizationInfo.userRoles[it]
                                } ?: Role.NONE
                                +role.formattedName
                            }
                        }
                    }
                }
            }
        }
    }
}
