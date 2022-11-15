@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.modal.displayModalWithClick
import csstype.ClassName
import org.w3c.fetch.Response
import react.*
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div

val actionButton: FC<ActionProps> = FC { props ->
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
        isOpen = windowOpenness.isOpen(),
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
        clickBuilder = {
            if (props.conditionClick && !isError) {
                div {
                    className = ClassName("d-sm-flex justify-content-center form-check")
                    div {
                        className = ClassName("d-sm-flex justify-content-center form-check")
                        div {
                            ReactHTML.input {
                                className = ClassName("click")
                                type = InputType.checkbox
                                value = isClickMode
                                id = "click"
                                checked = isClickMode
                                onChange = {
                                    setClickMode(!isClickMode)
                                }
                            }
                        }
                        div {
                            ReactHTML.label {
                                className = ClassName("click")
                                htmlFor = "click"
                                +props.clickMessage
                            }
                        }
                    }
                }
            }
        }
    )
}

/**
 * Button with modal for some action
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
    @Suppress("TYPE_ALIAS")
    var sendRequest: (Boolean) -> DeferredRequestAction<Response>
}
