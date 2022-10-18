/**
 * Utilities for cli args parsing
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.domain.Role
import com.saveourtool.save.filters.TestExecutionFilters
import com.saveourtool.save.frontend.components.basic.nameFiltersRow
import com.saveourtool.save.frontend.components.modal.displayModalWithClick
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import csstype.ClassName
import org.w3c.fetch.Response
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.tr

/**
 * Button for delete organization
 *
 * @return noting
 */


data class DisplayText(

    var modalTitle: String = "",

    var modalMessage: String ="",

    var errorTitle: String = "",

    var clickMessage: String = "",

    ) {
    companion object {
        fun deleteProject(name: String) = DisplayText(
            modalTitle = "Warning: deletion of project",
            modalMessage = "You are about to delete project $name. Are you sure?",
            errorTitle = "You cannot delete $name",
            clickMessage = "Do you want to ban an project $name?",
        )
        fun deleteOrganization(name: String) = DisplayText(
            modalTitle = "Warning: deletion of organization",
            modalMessage = "You are about to delete organization $name. Are you sure?",
            errorTitle = "You cannot delete $name",
            clickMessage = "Do you want to ban an organization $name?",
        )
        fun recoveryProject(name: String) = DisplayText(
            modalTitle = "Warning: recovery of project",
            modalMessage = "You are about to recovery project $name. Are you sure?",
            errorTitle = "You cannot recover $name",
            clickMessage = "Do you want to ban an project $name?",
        )
        fun recoveryOrganization(name: String) = DisplayText(
            modalTitle = "Warning: recovery of organization",
            modalMessage = "You are about to recovery organization $name. Are you sure?",
            errorTitle = "You cannot recover $name",
            clickMessage = "Do you want to ban an organization $name?",
        )
        val empty = DisplayText(modalTitle = "", modalMessage = "", errorTitle = "", clickMessage = "")
    }
}


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

    var displayText: DisplayText

    var modalButtons: ( action: () -> Unit , WindowOpenness , ChildrenBuilder) -> Unit

    var clickButtons: ( action: () -> Unit , WindowOpenness , ChildrenBuilder) -> Unit

    var role: Role
}


val actionButton: FC<TemplateActionProps> = FC {props->
    val windowOpenness = useWindowOpenness()
    val (displayTitle, setDisplayTitle) = useState(props.displayText.modalTitle)
    val (displayMessage, setDisplayMessage) = useState(props.displayText.modalMessage)
    val (isError, setError) = useState(false)

    val action = useDeferredRequest {
        val responseFromDeleteOrganization = props.sendRequest(this)
        if (responseFromDeleteOrganization.ok) {
            props.onActionSuccess()
        } else {
            setDisplayTitle(props.displayText.errorTitle)
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
                setDisplayTitle(props.displayText.modalTitle)
                setDisplayMessage(props.displayText.modalMessage)
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
                buttonBuilder("Ok") {
                    windowOpenness.closeWindow()
                    setError(false)
                }
            } else {
                props.modalButtons(action, windowOpenness, this)
            }
        },
        textClickIcon = props.displayText.clickMessage,
        conditionClickIcon = if(!isError) { { props.role.isHigherOrEqualThan(Role.SUPER_ADMIN) } } else { {false} },
        clickButtonBuilder = { props.clickButtons(action, windowOpenness, this) }
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


val deleteOrganizationButton: FC<DeleteOrganizationButtonProps> = FC {props ->
    val (isClickMode, setClickMode) = useState(false)
    actionButton {
        name = props.organizationName
        onActionSuccess = props.onDeletionSuccess
        buttonStyleBuilder = props.buttonStyleBuilder
        classes = props.classes
        sendRequest = { WithRequestStatusContext ->
            val bannedMode = if (isClickMode) "banned" else "deleted"
            with(WithRequestStatusContext) {
                delete (
                    "$apiUrl/organizations/${props.organizationName}/delete?status=${bannedMode}",
                    headers = jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                    errorHandler = ::noopResponseHandler,
                )
            }
        }
        displayText = DisplayText.deleteOrganization(props.organizationName)
        modalButtons = { action, window, childrenBuilder ->
            with(childrenBuilder) {
                buttonBuilder("Yes, delete ${props.organizationName}", "danger") {
                    action()
                    window.closeWindow()
                }
                buttonBuilder("Cancel") {
                    window.closeWindow()
                }
            }
        }
        clickButtons = { action, window, childrenBuilder ->
            with(childrenBuilder) {
                buttonBuilder("Yes, ban ${props.organizationName}", "danger") {
                    setClickMode(true)
                    action()
                    window.closeWindow()
                    setClickMode(false)
                }
            }
        }
        role = props.userRole
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
    val (isClickMode, setClickMode) = useState(false)
    actionButton {
        name = props.projectName
        onActionSuccess = props.onDeletionSuccess
        buttonStyleBuilder = props.buttonStyleBuilder
        classes = props.classes
        sendRequest = { WithRequestStatusContext ->
            val bannedMode = if (isClickMode) "banned" else "deleted"
            with(WithRequestStatusContext) {
                delete (
                    url ="$apiUrl/projects/${props.organizationName}/${props.projectName}/delete?status=${bannedMode}",
                    headers = jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                    errorHandler = ::noopResponseHandler,
                )
            }
        }
        displayText = DisplayText.deleteProject(props.projectName)
        modalButtons = { action, window, childrenBuilder ->
            with(childrenBuilder){
                buttonBuilder("Yes, delete ${props.projectName}", "danger") {
                    action()
                    window.closeWindow()
                }
                buttonBuilder("Cancel") {
                    window.closeWindow()
                }
            }
        }
        clickButtons = { action, window, childrenBuilder ->
            with(childrenBuilder) {
                buttonBuilder("Yes, ban ${props.projectName} in ${props.organizationName}", "danger") {
                    setClickMode(true)
                    action()
                    window.closeWindow()
                    setClickMode(false)
                }
            }
        }
        role = props.userRole
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


val recoveryOrganizationButton: FC<RecoveryOrganizationButtonProps> = FC {props ->
    val (isClickMode, setClickMode) = useState(false)
    actionButton {
        name = props.organizationName
        onActionSuccess = props.onRecoverySuccess
        buttonStyleBuilder = props.buttonStyleBuilder
        classes = props.classes
        sendRequest = { WithRequestStatusContext ->
            with(WithRequestStatusContext) {
                post (
                    url ="$apiUrl/organization/${props.organizationName}/recovery",
                    headers = jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                    responseHandler = ::noopResponseHandler,
                )
            }
        }
        displayText = DisplayText.recoveryOrganization(props.organizationName)
        modalButtons = { action, window, childrenBuilder ->
            with(childrenBuilder){
                buttonBuilder("Yes, recover ${props.organizationName}", "danger") {
                    action()
                    window.closeWindow()
                }
                buttonBuilder("Cancel") {
                    window.closeWindow()
                }
            }
        }
        clickButtons = { action, window, childrenBuilder ->
            with(childrenBuilder) {
                buttonBuilder("Yes, recover from ban ${props.organizationName}", "danger") {
                    setClickMode(true)
                    action()
                    window.closeWindow()
                    setClickMode(false)
                }
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
    val (isClickMode, setClickMode) = useState(false)
    actionButton {
        name = props.projectName
        onActionSuccess = props.onRecoverySuccess
        buttonStyleBuilder = props.buttonStyleBuilder
        classes = props.classes
        sendRequest = { WithRequestStatusContext ->
            with(WithRequestStatusContext) {
                post (
                    url ="$apiUrl/projects/${props.organizationName}/${props.projectName}/recovery?role=${props.userRole}",
                    headers = jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                    responseHandler = ::noopResponseHandler,
                )
            }
        }
        displayText = DisplayText.recoveryProject(props.projectName)
        modalButtons = { action, window, childrenBuilder ->
            with(childrenBuilder){
                buttonBuilder("Yes, recover ${props.projectName}", "danger") {
                    action()
                    window.closeWindow()
                }
                buttonBuilder("Cancel") {
                    window.closeWindow()
                }
            }
        }
        clickButtons = { action, window, childrenBuilder ->
            with(childrenBuilder) {
                buttonBuilder("Yes, recover from ban ${props.projectName} in ${props.organizationName}", "danger") {
                    setClickMode(true)
                    action()
                    window.closeWindow()
                    setClickMode(false)
                }
            }
        }
        role = props.userRole
    }
}