@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.frontend.components.basic.manageUserRoleCardComponent
import com.saveourtool.save.frontend.utils.useGlobalRoleWarningCallback
import com.saveourtool.save.info.UserInfo
import csstype.ClassName

import org.w3c.fetch.Response
import react.*

import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label

private val organizationPermissionManagerCard = manageUserRoleCardComponent()

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
     * Callback to delete organization
     */
    var deleteOrganizationCallback: () -> Unit

    /**
     * Callback to show error message
     */
    var updateErrorMessage: (Response) -> Unit

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
    val (wasConfirmationModalShown, showGlobalRoleWarning) = useGlobalRoleWarningCallback(props.updateNotificationMessage)
    div {
        className = ClassName("row justify-content-center mb-2")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Users"
            }
            organizationPermissionManagerCard {
                selfUserInfo = props.currentUserInfo
                groupPath = organizationPath
                groupType = "organization"
                this.wasConfirmationModalShown = wasConfirmationModalShown
                updateErrorMessage = props.updateErrorMessage
                getUserGroups = { it.organizations }
                this.showGlobalRoleWarning = showGlobalRoleWarning
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
                this.wasConfirmationModalShown = wasConfirmationModalShown
                updateErrorMessage = props.updateErrorMessage
                this.showGlobalRoleWarning = showGlobalRoleWarning
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
                if (props.selfRole == Role.SUPER_ADMIN) {
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
                    button {
                        type = ButtonType.button
                        className = ClassName("btn btn-sm btn-danger")
                        disabled = !props.selfRole.hasDeletePermission()
                        onClick = {
                            props.deleteOrganizationCallback()
                        }
                        +"Delete organization"
                    }
                }
            }
        }
    }
}
