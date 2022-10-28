/**
 * Utilities for cli args parsing
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.modal.displayModalWithClick
import com.saveourtool.save.frontend.utils.*
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



external interface ActionProps:Props {
    var typeOfOperation: TypeOfAction

    var title: String

    var errorTitle: String

    var message: String

    var clickMessage: String

    /**
     * lambda to change [organizationName]
     */
    var onActionSuccess: (Boolean) -> Unit

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

    var sendRequest: (TypeOfAction) -> DeferredRequestAction<Response>
}


val actionButton: FC<ActionProps> = FC {props->
    val windowOpenness = useWindowOpenness()
    val (displayTitle, setDisplayTitle) = useState(props.title)
    val (displayMessage, setDisplayMessage) = useState(props.message)
    val (isError, setError) = useState(false)


    val action = useDeferredRequest {
        val response = props.sendRequest(props.typeOfOperation)(this) {}
        if (response.ok){
            props.onActionSuccess(props.typeOfOperation.clickMode)
        } else {
            setDisplayTitle(props.errorTitle)
            setDisplayMessage(response.unpackMessage())
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

    displayModalWithClick (
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
            props.typeOfOperation.changeClickMode(it)
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

    fun changeClickMode(elem: Boolean) {
        clickMode = elem
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
