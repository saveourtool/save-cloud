@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.Role
import com.saveourtool.save.info.UserInfo

import org.w3c.fetch.Response
import react.*
import react.dom.*

import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction

private val organizationPermissionManagerCard = manageUserRoleCardComponent()

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
}

/**
 * @param deleteOrganizationCallback
 * @param updateErrorMessage
 * @param updateNotificationMessage
 * @return ReactElement
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod"
)
fun organizationSettingsMenu(
    deleteOrganizationCallback: () -> Unit,
    updateErrorMessage: (Response) -> Unit,
    updateNotificationMessage: (String, String) -> Unit,
) = fc<OrganizationSettingsMenuProps> { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val organizationPath = props.organizationName
    val (wasConfirmationModalShown, setWasConfirmationModalShown) = useState(false)
    div("row justify-content-center mb-2") {
        // ===================== LEFT COLUMN =======================================================================
        div("col-4 mb-2 pl-0 pr-0 mr-2 ml-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Users"
            }
            child(organizationPermissionManagerCard) {
                attrs.selfUserInfo = props.currentUserInfo
                attrs.groupPath = organizationPath
                attrs.groupType = "organization"
                attrs.wasConfirmationModalShown = wasConfirmationModalShown
                attrs.updateErrorMessage = updateErrorMessage
                attrs.getUserGroups = { it.organizations }
                attrs.showGlobalRoleWarning = {
                    updateNotificationMessage(
                        "Super admin message",
                        "Keep in mind that you are super admin, so you are able to manage organization regardless of your organization permissions.",
                    )
                    setWasConfirmationModalShown(true)
                }
            }
        }
        // ===================== RIGHT COLUMN ======================================================================
        div("col-4 mb-2 pl-0 pr-0 mr-2 ml-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Main settings"
            }
            div("card card-body mt-0 pt-0 pr-0 pl-0") {
                div("row d-flex justify-content-center mt-3") {
                    div("col-3 d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-sm btn-danger") {
                            attrs.disabled = props.selfRole.isLowerThan(Role.OWNER)
                            attrs.onClickFunction = {
                                deleteOrganizationCallback()
                            }
                            +"Delete organization"
                        }
                    }
                }
            }
        }
    }
}
