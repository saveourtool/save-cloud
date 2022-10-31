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
    val (isClickMode, setClickMode) = useState(false)

    val action = useDeferredRequest {
        val response = props.sendRequest(isClickMode)(this)
        if (response.ok) {
            props.onActionSuccess(isClickMode)
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
            setClickMode(it)
        }
    )
}

/**
 * Button with modal for something action
 *
 * @return noting
 */
external interface ActionProps : Props {
    /**
     * Title of the modal
     */
    var title: String

    /**
     * Error title of the modal
     */
    var errorTitle: String

    /**
     * Message of the modal
     */
    var message: String

    /**
     * Message when clicked
     */
    var clickMessage: String

    /**
     * If the action (request) is successful, this is done
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
     * Modal buttons
     */
    @Suppress("TYPE_ALIAS")
    var modalButtons: (action: () -> Unit, WindowOpenness, ChildrenBuilder) -> Unit

    /**
     * Condition for click
     */
    var conditionClick: Boolean

    /**
     * Request
     */
    var sendRequest: (Boolean) -> DeferredRequestAction<Response>
}
