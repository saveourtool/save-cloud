/**
 * Utilities for cli args parsing
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.modal.displayModalWithClick
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import csstype.ClassName
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
    var typeOfOperation: TypeOfAction

    var title: String

    var errorTitle: String

    var message: String

    var clickMessage: String

    var url: String

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

    var modalButtons: ( action: () -> Unit , WindowOpenness , ChildrenBuilder) -> Unit

    var conditionClick: Boolean
}


val actionButton: FC<TemplateActionProps> = FC {props->
    val windowOpenness = useWindowOpenness()
    val (displayTitle, setDisplayTitle) = useState(props.title)
    val (displayMessage, setDisplayMessage) = useState(props.message)
    val (isError, setError) = useState(false)

    val action = useDeferredRequest {
        val responseFromDeleteOrganization = post(
            url = props.typeOfOperation.createRequest(props.url),
            headers = jsonHeaders,
            body = undefined,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )
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
                setDisplayTitle(props.title)
                setDisplayMessage(props.message)
                windowOpenness.openWindow()
            }
        }
    }

    displayModalWithClick(
        title = displayTitle,
        message = displayMessage,
        clickMessage = props.clickMessage,
        isOpen = windowOpenness.isOpen(),
        isErrorMode = isError,
        conditionClickIcon = props.conditionClick,
        onCloseButtonPressed = windowOpenness.closeWindowAction(),
        buttonBuilder = {
            if (isError) {
                buttonBuilder("Ok") {
                    windowOpenness.closeWindow()
                    setError(false)
                }
            } else {
                props.modalButtons(action, windowOpenness, this)
            }
        },
        changeClickMode = {
            props.typeOfOperation.changeClickMode()
        }
    )
}



enum class TypeOfAction {
    DELETE_PROJECT,
    RECOVERY_PROJECT,
    DELETE_ORGANIZATION,
    RECOVERY_ORGANIZATION,
    ;

    var clickMode: Boolean = false

    fun changeClickMode() {
        clickMode = !clickMode
    }
    fun createRequest(url: String): String {
        return when (this) {
            DELETE_ORGANIZATION, DELETE_PROJECT ->
                buildString {
                    append(url)
                    append("?status=")
                    if (clickMode) append("banned") else append("deleted")
                }
            else -> url
        }
    }

}
//
///**
// * DeleteOrganizationButton props
// */
//external interface DeleteOrganizationButtonProps : Props {
//    /**
//     * All filters in one class property [organizationName]
//     */
//    var organizationName: String
//
//    /**
//     * lambda to change [organizationName]
//     */
//    var onDeletionSuccess: () -> Unit
//
//    /**
//     * Button View
//     */
//    var buttonStyleBuilder: (ChildrenBuilder) -> Unit
//
//    /**
//     * Classname for the button
//     */
//    var classes: String
//
//    /**
//     * User role
//     */
//    var userRole: Role
//}
//
//
//val deleteOrganizationButton: FC<DeleteOrganizationButtonProps> = FC {props ->
//    val (isClickMode, setClickMode) = useState(false)
//    actionButton {
//        name = props.organizationName
//        onActionSuccess = props.onDeletionSuccess
//        buttonStyleBuilder = props.buttonStyleBuilder
//        classes = props.classes
//        sendRequest = { WithRequestStatusContext ->
//            val bannedMode = if (isClickMode) "banned" else "deleted"
//            with(WithRequestStatusContext) {
//                delete (
//                    "$apiUrl/organizations/${props.organizationName}/delete?status=${bannedMode}",
//                    headers = jsonHeaders,
//                    body = undefined,
//                    loadingHandler = ::noopLoadingHandler,
//                    errorHandler = ::noopResponseHandler,
//                )
//            }
//        }
//        displayText = DisplayText.deleteOrganization(props.organizationName)
//        modalButtons = { action, window, childrenBuilder ->
//            with(childrenBuilder) {
//                buttonBuilder("Yes, delete ${props.organizationName}", "danger") {
//                    action()
//                    window.closeWindow()
//                }
//                buttonBuilder("Cancel") {
//                    window.closeWindow()
//                }
//            }
//        }
//        clickButtons = { action, window, childrenBuilder ->
//            with(childrenBuilder) {
//                buttonBuilder("Yes, ban ${props.organizationName}", "danger") {
//                    setClickMode(true)
//                    action()
//                    window.closeWindow()
//                    setClickMode(false)
//                }
//            }
//        }
//        isClick = isClickMode
//        changeClickMode = {
//            setClickMode(it)
//        }
//        role = props.userRole
//    }
//}
//
//
//external interface DeleteProjectButtonProps : Props {
//    var organizationName: String
//    /**
//     * All filters in one class property [organizationName]
//     */
//    var projectName: String
//
//    /**
//     * lambda to change [organizationName]
//     */
//    var onDeletionSuccess: () -> Unit
//
//    /**
//     * Button View
//     */
//    var buttonStyleBuilder: (ChildrenBuilder) -> Unit
//
//    /**
//     * Classname for the button
//     */
//    var classes: String
//
//    /**
//     * User role
//     */
//    var userRole: Role
//}
//
//
//val deleteProjectButton: FC<DeleteProjectButtonProps> = FC {props->
//    val (isClickMode, setClickMode) = useState(false)
//    actionButton {
//        name = props.projectName
//        onActionSuccess = props.onDeletionSuccess
//        buttonStyleBuilder = props.buttonStyleBuilder
//        classes = props.classes
//        sendRequest = { WithRequestStatusContext ->
//            val bannedMode = if (isClickMode) "banned" else "deleted"
//            with(WithRequestStatusContext) {
//                delete (
//                    url ="$apiUrl/projects/${props.organizationName}/${props.projectName}/delete?status=${bannedMode}",
//                    headers = jsonHeaders,
//                    body = undefined,
//                    loadingHandler = ::noopLoadingHandler,
//                    errorHandler = ::noopResponseHandler,
//                )
//            }
//        }
//        displayText = DisplayText.deleteProject(props.projectName)
//        modalButtons = { action, window, childrenBuilder ->
//            with(childrenBuilder){
//                buttonBuilder("Yes, delete ${props.projectName}", "danger") {
//                    action()
//                    window.closeWindow()
//                }
//                buttonBuilder("Cancel") {
//                    window.closeWindow()
//                }
//            }
//        }
//        clickButtons = { action, window, childrenBuilder ->
//            with(childrenBuilder) {
//                buttonBuilder("Yes, ban ${props.projectName} in ${props.organizationName}", "danger") {
//                    setClickMode(true)
//                    action()
//                    window.closeWindow()
//                    setClickMode(false)
//                }
//            }
//        }
//        role = props.userRole
//    }
//}
//
//
//external interface RecoveryOrganizationButtonProps : Props {
//    /**
//     * All filters in one class property [organizationName]
//     */
//    var organizationName: String
//
//    /**
//     * lambda to change [organizationName]
//     */
//    var onRecoverySuccess: () -> Unit
//
//    /**
//     * User role
//     */
//    var userRole: Role
//
//    /**
//     * Button View
//     */
//    var buttonStyleBuilder: (ChildrenBuilder) -> Unit
//
//    /**
//     * Classname for the button
//     */
//    var classes: String
//}
//
//
//val recoveryOrganizationButton: FC<RecoveryOrganizationButtonProps> = FC {props ->
//    val (isClickMode, setClickMode) = useState(false)
//    actionButton {
//        name = props.organizationName
//        onActionSuccess = props.onRecoverySuccess
//        buttonStyleBuilder = props.buttonStyleBuilder
//        classes = props.classes
//        sendRequest = { WithRequestStatusContext ->
//            with(WithRequestStatusContext) {
//                post (
//                    url ="$apiUrl/organization/${props.organizationName}/recovery",
//                    headers = jsonHeaders,
//                    body = undefined,
//                    loadingHandler = ::noopLoadingHandler,
//                    responseHandler = ::noopResponseHandler,
//                )
//            }
//        }
//        displayText = DisplayText.recoveryOrganization(props.organizationName)
//        modalButtons = { action, window, childrenBuilder ->
//            with(childrenBuilder){
//                buttonBuilder("Yes, recover ${props.organizationName}", "danger") {
//                    action()
//                    window.closeWindow()
//                }
//                buttonBuilder("Cancel") {
//                    window.closeWindow()
//                }
//            }
//        }
//        clickButtons = { action, window, childrenBuilder ->
//            with(childrenBuilder) {
//                buttonBuilder("Yes, recover from ban ${props.organizationName}", "danger") {
//                    setClickMode(true)
//                    action()
//                    window.closeWindow()
//                    setClickMode(false)
//                }
//            }
//        }
//        role = props.userRole
//    }
//}
//
//
//
//external interface RecoveryProjectButtonProps : Props {
//    /**
//     * All filters in one class property [organizationName]
//     */
//    var organizationName: String
//
//    /**
//     * All filters in one class property [organizationName]
//     */
//    var projectName: String
//
//    /**
//     * lambda to change [organizationName]
//     */
//    var onRecoverySuccess: () -> Unit
//
//    /**
//     * User role
//     */
//    var userRole: Role
//
//    /**
//     * Button View
//     */
//    var buttonStyleBuilder: (ChildrenBuilder) -> Unit
//
//    /**
//     * Classname for the button
//     */
//    var classes: String
//}
//
//
//val recoveryProjectButton: FC<RecoveryProjectButtonProps> = FC {props->
//    val (isClickMode, setClickMode) = useState(false)
//    actionButton {
//        name = props.projectName
//        onActionSuccess = props.onRecoverySuccess
//        buttonStyleBuilder = props.buttonStyleBuilder
//        classes = props.classes
//        sendRequest = { WithRequestStatusContext ->
//            with(WithRequestStatusContext) {
//                post (
//                    url ="$apiUrl/projects/${props.organizationName}/${props.projectName}/recovery?role=${props.userRole}",
//                    headers = jsonHeaders,
//                    body = undefined,
//                    loadingHandler = ::noopLoadingHandler,
//                    responseHandler = ::noopResponseHandler,
//                )
//            }
//        }
//        displayText = DisplayText.recoveryProject(props.projectName)
//        modalButtons = { action, window, childrenBuilder ->
//            with(childrenBuilder){
//                buttonBuilder("Yes, recover ${props.projectName}", "danger") {
//                    action()
//                    window.closeWindow()
//                }
//                buttonBuilder("Cancel") {
//                    window.closeWindow()
//                }
//            }
//        }
//        clickButtons = { action, window, childrenBuilder ->
//            with(childrenBuilder) {
//                buttonBuilder("Yes, recover from ban ${props.projectName} in ${props.organizationName}", "danger") {
//                    setClickMode(true)
//                    action()
//                    window.closeWindow()
//                    setClickMode(false)
//                }
//            }
//        }
//        role = props.userRole
//    }
//}