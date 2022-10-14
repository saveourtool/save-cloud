/**
 * Utilities for cli args parsing
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.components.modal.displayModalWithClick
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import csstype.ClassName
import org.w3c.fetch.Response
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div

/**
 * Button for delete organization
 *
 * @return noting
 */


external interface TemplateActionProps:Props {
    /**
     * All filters in one class property [organizationName]
     */
    var name: String

    /**
     * lambda to change [organizationName]
     */
    var onActionSuccess: () -> Unit

    /**
     * Button View
     */
    var buttonStyleBuilder: (ChildrenBuilder) -> Unit

    /**
     * Classname for the button
     */
    var classes: String


    var sendRequest: suspend (WithRequestStatusContext) -> Response

    /**
     * All filters in one class property [organizationName]
     */
    var modalTitle: String

    /**
     * All filters in one class property [organizationName]
     */
    var modalMessage: String

    var errorTitle: String

    var clickMessage: String

    var modalButtons: ( action: () -> Unit , WindowOpenness) -> Unit

    var role: Role

    var changeBannedMode: (Boolean) -> Unit
}



val actionButton: FC<TemplateActionProps> = FC {props->
    val windowOpenness = useWindowOpenness()
    val (displayTitle, setDisplayTitle) = useState(props.modalTitle)
    val (displayMessage, setDisplayMessage) = useState(props.modalMessage)
    val (isError, setError) = useState(false)

    val action = useDeferredRequest {
        val responseFromDeleteOrganization = props.sendRequest(this)
        if (responseFromDeleteOrganization.ok) {
            props.onActionSuccess()
        } else {
            setDisplayTitle(props.errorTitle)
            setDisplayMessage(responseFromDeleteOrganization.unpackMessage())
            setError(true)
            windowOpenness.openWindow()
        }
    }

    div {
        button {
            type = ButtonType.button
            className = ClassName(props.classes)
            props.buttonStyleBuilder(this)
            onClick = {
                setDisplayTitle(props.modalTitle)
                setDisplayMessage(props.modalMessage)
                windowOpenness.openWindow()
            }
        }
    }

    displayModalWithClick(
        isOpen = windowOpenness.isOpen(),
        title = displayTitle,
        message = displayMessage,
        onCloseButtonPressed = windowOpenness.closeWindowAction(),
        buttonBuilder = {
            if (isError){
                buttonBuilder("Ok") { windowOpenness.closeWindow() }
            } else {
                props.modalButtons(action, windowOpenness)
            }
        },
        textClickIcon = props.clickMessage,
        conditionClickIcon = { props.role.isHigherOrEqualThan(Role.SUPER_ADMIN) },
        clickButtonBuilder = {
            buttonBuilder("Banned ${props.name}", "danger") {
                props.changeBannedMode(true)
                windowOpenness.closeWindow()
            }
        }
    )
}

/**
 * DeleteOrganizationButton props
 */
external interface DeleteOrganizationButtonProps : Props {
    /**
     * All filters in one class property [organizationName]
     */
    var organizationName: String

    /**
     * lambda to change [organizationName]
     */
    var onDeletionSuccess: () -> Unit

    /**
     * Button View
     */
    var buttonStyleBuilder: (ChildrenBuilder) -> Unit

    /**
     * Classname for the button
     */
    var classes: String

    /**
     * User role
     */
    var userRole: Role
}


val deleteOrganizationButton: FC<DeleteOrganizationButtonProps> = FC {props->
    val (isBannedMode, setBannedMode) = useState(false)
    actionButton {
        name = props.organizationName
        onActionSuccess = props.onDeletionSuccess
        buttonStyleBuilder = props.buttonStyleBuilder
        classes = props.classes
        sendRequest = {
            val bannedMode = if (isBannedMode) "banned" else "deleted"
            with(it) {
                delete (
                    "$apiUrl/organizations/${props.organizationName}/delete?status=${bannedMode}",
                    headers = jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                    errorHandler = ::noopResponseHandler,
                )
            }
        }
        modalTitle = "Warning: deletion of organization"
        modalMessage = "You are about to delete organization ${props.organizationName}. Are you sure?"
        errorTitle = "You cannot delete ${props.organizationName}"
        modalButtons = { action, window->
            buttonBuilder("Yes, delete ${props.organizationName}", "danger") {
                action()
                window.closeWindow()
            }
            buttonBuilder("Cancel") {
                window.closeWindow()
            }
        }
        role = props.userRole
        changeBannedMode = { setBannedMode(it) }
    }
}


external interface DeleteProjectButtonProps : Props {
    var organizationName: String
    /**
     * All filters in one class property [organizationName]
     */
    var projectName: String

    /**
     * lambda to change [organizationName]
     */
    var onDeletionSuccess: () -> Unit

    /**
     * Button View
     */
    var buttonStyleBuilder: (ChildrenBuilder) -> Unit

    /**
     * Classname for the button
     */
    var classes: String

    /**
     * User role
     */
    var userRole: Role
}


val deleteProjectButton: FC<DeleteProjectButtonProps> = FC {props->
    val (isBannedMode, setBannedMode) = useState(false)
    actionButton {
        name = props.projectName
        onActionSuccess = props.onDeletionSuccess
        buttonStyleBuilder = props.buttonStyleBuilder
        classes = props.classes
        sendRequest = {
            val bannedMode = if (isBannedMode) "banned" else "deleted"
            with(it) {
                delete (
                    url ="$apiUrl/projects/${props.organizationName}/${props.projectName}/delete?status=${bannedMode}",
                    headers = jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                    errorHandler = ::noopResponseHandler,
                )
            }
        }
        modalTitle = "Warning: deletion of project"
        modalMessage = "You are about to delete project ${props.organizationName} in organization ${props.organizationName}. Are you sure?"
        errorTitle = "You cannot delete ${props.projectName}"
        modalButtons = { action, window->
            buttonBuilder("Yes, delete ${props.projectName}", "danger") {
                action()
                window.closeWindow()
            }
            buttonBuilder("Cancel") {
                window.closeWindow()
            }
        }
        role = props.userRole
        changeBannedMode = { setBannedMode(it) }
    }
}


external interface RecoveryOrganizationButtonProps : Props {
    /**
     * All filters in one class property [organizationName]
     */
    var organizationName: String

    /**
     * lambda to change [organizationName]
     */
    var onRecoverySuccess: () -> Unit

    /**
     * User role
     */
    var userRole: Role

    /**
     * Button View
     */
    var buttonStyleBuilder: (ChildrenBuilder) -> Unit

    /**
     * Classname for the button
     */
    var classes: String
}


val recoveryOrganizationButton: FC<RecoveryOrganizationButtonProps> = FC {props->
    actionButton {
        name = props.organizationName
        onActionSuccess = props.onRecoverySuccess
        buttonStyleBuilder = props.buttonStyleBuilder
        classes = props.classes
        sendRequest = {
            with(it) {
                post (
                    url ="$apiUrl/organization/${props.organizationName}/recovery?role=${props.userRole}",
                    headers = jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                    responseHandler = ::noopResponseHandler,
                )
            }
        }
        modalTitle = "Warning: recovery of organization"
        modalMessage = "You are about to recovery organization ${props.organizationName}. Are you sure?"
        errorTitle = "You cannot recover ${props.organizationName}"
        modalButtons = { action, window->
            buttonBuilder("Yes, recover ${props.organizationName}", "danger") {
                action()
                window.closeWindow()
            }
            buttonBuilder("Cancel") {
                window.closeWindow()
            }
        }
        role = props.userRole
    }
}



external interface RecoveryProjectButtonProps : Props {
    /**
     * All filters in one class property [organizationName]
     */
    var organizationName: String

    /**
     * All filters in one class property [organizationName]
     */
    var projectName: String

    /**
     * lambda to change [organizationName]
     */
    var onRecoverySuccess: () -> Unit

    /**
     * User role
     */
    var userRole: Role

    /**
     * Button View
     */
    var buttonStyleBuilder: (ChildrenBuilder) -> Unit

    /**
     * Classname for the button
     */
    var classes: String
}


val recoveryProjectButton: FC<RecoveryProjectButtonProps> = FC {props->
    actionButton {
        name = props.projectName
        onActionSuccess = props.onRecoverySuccess
        buttonStyleBuilder = props.buttonStyleBuilder
        classes = props.classes
        sendRequest = {
            with(it) {
                post (
                    url ="$apiUrl/projects/${props.organizationName}/${props.projectName}/recovery?role=${props.userRole}",
                    headers = jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                    responseHandler = ::noopResponseHandler,
                )
            }
        }
        modalTitle = "Warning: recovery of project"
        modalMessage = "You are about to recovery project ${props.organizationName} in organization ${props.organizationName}. Are you sure?"
        errorTitle = "You cannot recover ${props.projectName}"
        modalButtons = { action, window->
            buttonBuilder("Yes, recover ${props.projectName}", "danger") {
                action()
                window.closeWindow()
            }
            buttonBuilder("Cancel") {
                window.closeWindow()
            }
        }
        role = props.userRole
    }
}