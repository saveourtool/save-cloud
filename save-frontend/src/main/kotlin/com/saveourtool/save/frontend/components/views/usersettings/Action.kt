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

val actionButton: FC<ActionProps> = FC {props ->
    val windowOpenness = useWindowOpenness()
    val (displayTitle, setDisplayTitle) = useState(props.title)
    val (displayMessage, setDisplayMessage) = useState(props.message)
    val (isError, setError) = useState(false)

    val action = useDeferredRequest {
        val response = props.sendRequest(props.typeOfOperation)(this) {}
        if (response.ok) {
            props.onActionSuccess(props.typeOfOperation.isClickMode)
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

    displayModalWithClick(
        title = displayTitle,
        message = displayMessage,
        clickMessage = props.clickMessage,
        isOpen = windowOpenness.isOpen(),
        conditionClickIcon = props.conditionClick && !isError,
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

/**
 * Button with modal for action with something
 *
 * @return noting
 */
external interface ActionProps : Props {
    /**
     * type of action
     */
    var typeOfOperation: TypeOfAction

    /**
     * title of the modal
     */
    var title: String

    /**
     * error title of the modal
     */
    var errorTitle: String

    /**
     * message of the modal
     */
    var message: String

    /**
     * message when clicked
     */
    var clickMessage: String

    /**
     * if the action (request) is successful, this is done
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

    /**
     * modal buttons
     */
    @Suppress("TYPE_ALIAS")
    var modalButtons: (action: () -> Unit, WindowOpenness, ChildrenBuilder) -> Unit

    /**
     * condition for click
     */
    var conditionClick: Boolean

    /**
     * request
     */
    var sendRequest: (TypeOfAction) -> DeferredRequestAction<Response>
}

/**
 * Any type of action
 */
enum class TypeOfAction {
    DELETE_ORGANIZATION,
    DELETE_PROJECT,
    RECOVERY_ORGANIZATION,
    RECOVERY_PROJECT,
    ;

    /**
     * is click mode
     */
    var isClickMode: Boolean = false

    /**
     * Changed click mode
     *
     * @param elem
     */
    fun changeClickMode(elem: Boolean) {
        isClickMode = elem
    }

    /**
     * Generates a new url based on the old (basic) one
     *
     * @param url
     * @return new_url
     */
    fun createRequestUrl(url: String): String {
        val type = this
        return buildString {
            append(url)
            when (type) {
                DELETE_ORGANIZATION, DELETE_PROJECT -> {
                    append("?status=")
                    if (isClickMode) append("banned") else append("deleted")
                }
                else -> {}
            }
        }
    }
}
