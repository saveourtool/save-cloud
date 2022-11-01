@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.frontend.components.basic.manageUserRoleCardComponent
import com.saveourtool.save.frontend.components.views.usersettings.responseDeleteOrganization
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.actionButton
import com.saveourtool.save.frontend.utils.isSuperAdmin
import com.saveourtool.save.info.UserInfo

import csstype.ClassName
import org.w3c.fetch.Response
import react.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label

import kotlinx.browser.window

private val organizationGitCredentialsManageCard = manageGitCredentialsCardComponent()

/**
 * SETTINGS tab in OrganizationView
 */
val organizationSettingsMenu = organizationSettingsMenu()

/**
 * OrganizationSettingsMenu component props
 */
external interface OrganizationSettingsMenuProps : Props {
    /**
     * Current organization settings
     */
    var organizationName: String

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo

    /**
     * [Role] of user that is observing this component
     */
    var selfRole: Role

    /**
     * Callback to show error message
     */
    @Suppress("TYPE_ALIAS")
    var updateErrorMessage: (Response, String) -> Unit

    /**
     * Callback to show notification message
     */
    var updateNotificationMessage: (String, String) -> Unit

    /**
     * Current organization
     */
    var organization: Organization

    /**
     * Callback invoked in order to change canCreateContests flag
     */
    var onCanCreateContestsChange: (Boolean) -> Unit
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod"
)
private fun organizationSettingsMenu() = FC<OrganizationSettingsMenuProps> { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val organizationPath = props.organizationName
    div {
        className = ClassName("row justify-content-center mb-2")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Users"
            }
            manageUserRoleCardComponent {
                selfUserInfo = props.currentUserInfo
                groupPath = organizationPath
                groupType = "organization"
                getUserGroups = { it.organizations }
            }
        }
        // ===================== RIGHT COLUMN ======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Git credentials"
            }
            organizationGitCredentialsManageCard {
                selfUserInfo = props.currentUserInfo
                organizationName = props.organizationName
                updateErrorMessage = props.updateErrorMessage
            }
        }
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Main settings"
            }
            div {
                className = ClassName("card card-body mt-0 p-0")
                if (props.selfRole.isSuperAdmin()) {
                    div {
                        className = ClassName("d-sm-flex justify-content-center form-check pl-3 pr-3 pt-3")
                        div {
                            input {
                                className = ClassName("form-check-input")
                                type = InputType.checkbox
                                value = props.organization.canCreateContests.toString()
                                id = "canCreateContestsCheckbox"
                                checked = props.organization.canCreateContests
                                onChange = {
                                    props.onCanCreateContestsChange(!props.organization.canCreateContests)
                                }
                            }
                        }
                        div {
                            label {
                                className = ClassName("form-check-label")
                                htmlFor = "canCreateContestsCheckbox"
                                +"Can create contests"
                            }
                        }
                    }
                }
                div {
                    className = ClassName("d-sm-flex align-items-center justify-content-center p-3")
                    actionButton {
                        title = "WARNING: You want to delete an organization"
                        errorTitle = "You cannot delete ${props.organizationName}"
                        message = "Are you sure you want to delete a ${props.organizationName}?"
                        clickMessage = "Change to ban mode"
                        onActionSuccess = {
                            window.location.href = "${window.location.origin}/"
                        }
                        buttonStyleBuilder = { childrenBuilder ->
                            with(childrenBuilder) {
                                +"Delete ${props.organizationName}"
                            }
                        }
                        classes = "btn btn-sm btn-danger"
                        modalButtons = { action, window, childrenBuilder ->
                            with(childrenBuilder) {
                                buttonBuilder(label = "Yes, delete ${props.organizationName}", style = "danger", classes = "mr-2") {
                                    action()
                                    window.closeWindow()
                                }
                                buttonBuilder("Cancel") {
                                    window.closeWindow()
                                }
                            }
                        }
                        sendRequest = { isClickMode ->
                            responseDeleteOrganization(isClickMode, props.organizationName)
                        }
                        conditionClick = props.selfRole.isHigherOrEqualThan(Role.SUPER_ADMIN)
                    }
                }
            }
        }
    }
}
