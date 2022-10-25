@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.modal

import com.saveourtool.save.frontend.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.externals.modal.Styles
import com.saveourtool.save.frontend.utils.WindowOpenness
import com.saveourtool.save.frontend.utils.buttonBuilder

import csstype.ClassName
import react.ChildrenBuilder
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.span
import react.useState

/**
 * Universal function to create modals with bootstrap styles inside react modals.
 *
 * @param isOpen modal openness indicator - should be in state
 * @param title title of the modal that will be shown in top-left corner
 * @param bodyBuilder callback that defined modal body content
 * @param classes classes that will be applied to bootstrap modal div
 * @param modalStyle [Styles] that will be applied to react modal
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS", "LAMBDA_IS_NOT_LAST_PARAMETER")
fun ChildrenBuilder.displayModal(
    isOpen: Boolean,
    title: String,
    bodyBuilder: ChildrenBuilder.() -> Unit,
    classes: String = "",
    modalStyle: Styles = mediumTransparentModalStyle,
    onCloseButtonPressed: (() -> Unit)? = null,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = modalStyle
        modalBuilder(title, classes, onCloseButtonPressed, bodyBuilder, buttonBuilder)
    }
}

/**
 * Universal function to create modals with bootstrap styles inside react modals.
 *
 * @param isOpen modal openness indicator - should be in state
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param modalStyle [Styles] that will be applied to react modal
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.displayModal(
    isOpen: Boolean,
    title: String,
    message: String,
    modalStyle: Styles = mediumTransparentModalStyle,
    onCloseButtonPressed: (() -> Unit)? = null,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = modalStyle
        modalBuilder(title, message, onCloseButtonPressed, buttonBuilder)
    }
}

/**
 * @param displayText
 * @param isOpen
 * @param modalStyle
 * @param onCloseButtonPressed
 * @param buttonBuilder
 * @param conditionClickIcon
 * @param clickButtonBuilder
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.displayModalWithClick(
    title: String,
    message: String,
    clickMessage: String,
    isErrorMode: Boolean,
    conditionClickIcon: Boolean,
    isOpen: Boolean,
    modalStyle: Styles = mediumTransparentModalStyle,
    onCloseButtonPressed: (() -> Unit)? = null,
    buttonBuilder: ChildrenBuilder.() -> Unit,
    changeClickMode: (Boolean) -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = modalStyle
        modalBuilderWithClick(
            title, message, clickMessage, isErrorMode, conditionClickIcon,
            onCloseButtonPressed, buttonBuilder, changeClickMode,
        )
    }
}

/**
 * Universal function to create modals with bootstrap styles inside react modals.
 *
 * @param opener [WindowOpenness]
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param modalStyle [Styles] that will be applied to react modal
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.displayModal(
    opener: WindowOpenness,
    title: String,
    message: String,
    modalStyle: Styles = mediumTransparentModalStyle,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    displayModal(opener.isOpen(), title, message, modalStyle, opener.closeWindowAction(), buttonBuilder)
}

/**
 * Universal function to create modals for confirmation.
 *
 * @param windowOpenness
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param modalStyle [Styles] that will be applied to react modal
 * @param successAction lambda for success action
 */
fun ChildrenBuilder.displayConfirmationModal(
    windowOpenness: WindowOpenness,
    title: String,
    message: String,
    modalStyle: Styles = mediumTransparentModalStyle,
    successAction: () -> Unit,
) {
    displayModal(
        isOpen = windowOpenness.isOpen(),
        title = title,
        message = message,
        modalStyle = modalStyle,
        onCloseButtonPressed = windowOpenness.closeWindowAction()
    ) {
        buttonBuilder("Ok") {
            successAction()
            windowOpenness.closeWindow()
        }
        buttonBuilder("Cancel", "secondary") {
            windowOpenness.closeWindow()
        }
    }
}

/**
 * Universal function to create modals for confirmation.
 *
 * @param windowOpenness
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param modalStyle [Styles] that will be applied to react modal
 */
fun ChildrenBuilder.displayInfoModal(
    windowOpenness: WindowOpenness,
    title: String,
    message: String,
    modalStyle: Styles = mediumTransparentModalStyle,
) {
    displayModal(
        isOpen = windowOpenness.isOpen(),
        title = title,
        message = message,
        modalStyle = modalStyle,
        onCloseButtonPressed = windowOpenness.closeWindowAction()
    ) {
        buttonBuilder("Ok") {
            windowOpenness.closeWindow()
        }
    }
}

/**
 * Universal function to create modals with bootstrap styles.
 *
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
fun ChildrenBuilder.modalBuilder(
    title: String,
    message: String,
    onCloseButtonPressed: (() -> Unit)?,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    modalBuilder(
        title = title,
        onCloseButtonPressed = onCloseButtonPressed,
        bodyBuilder = {
            h2 {
                className = ClassName("h6 text-gray-800 mb-2")
                +message
            }
        },
        buttonBuilder = buttonBuilder,
    )
}

/**
 * Universal function to create modals with bootstrap styles.
 *
 * @param title title of the modal that will be shown in top-left corner
 * @param classes
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param bodyBuilder lambda that generates body of modal
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
fun ChildrenBuilder.modalBuilder(
    title: String,
    classes: String = "",
    onCloseButtonPressed: (() -> Unit)?,
    bodyBuilder: ChildrenBuilder.() -> Unit,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    div {
        className = ClassName("modal-dialog $classes")
        div {
            className = ClassName("modal-content")
            div {
                className = ClassName("modal-header")
                h5 {
                    className = ClassName("modal-title")
                    +title
                }
                onCloseButtonPressed?.let {
                    button {
                        type = ButtonType.button
                        className = ClassName("close")
                        asDynamic()["data-dismiss"] = "modal"
                        ariaLabel = "Close"
                        span {
                            fontAwesomeIcon(icon = faTimesCircle)
                            onClick = { onCloseButtonPressed() }
                        }
                    }
                }
            }
            div {
                className = ClassName("modal-body")
                bodyBuilder()
            }
            div {
                className = ClassName("modal-footer")
                buttonBuilder()
            }
        }
    }
}

/**
 * @param displayText
 * @param onCloseButtonPressed
 * @param buttonBuilder
 * @param conditionClickIcon
 * @param clickButtonBuilder
 */
fun ChildrenBuilder.modalBuilderWithClick(
    title: String,
    message: String,
    clickMessage: String,
    isErrorMode: Boolean,
    conditionClickIcon: Boolean,
    onCloseButtonPressed: (() -> Unit)?,
    buttonBuilder: ChildrenBuilder.() -> Unit,
    changeClickMode: (Boolean) -> Unit,
) {
    val (click, setClick) = useState(false)
    modalBuilderWithClick(
        title = title,
        onCloseButtonPressed = onCloseButtonPressed,
        bodyBuilder = {
            h2 {
                className = ClassName("h6 text-gray-800 mb-2")
                +message
            }
        },
        buttonAfterClick = {
            if (conditionClickIcon && !isErrorMode) {
                div {
                    className = ClassName("d-sm-flex justify-content-center form-check")
                    div {
                        className = ClassName("d-sm-flex justify-content-center form-check")
                        div {
                            input {
                                className = ClassName("click")
                                type = InputType.checkbox
                                value = click
                                id = "click"
                                checked = click
                                onChange = {
                                    setClick(!click)
                                    changeClickMode(!click)
                                }
                            }
                        }
                        div {
                            label {
                                className = ClassName("click")
                                htmlFor = "click"
                                +clickMessage
                            }
                        }
                    }
                }
            }
        },
        buttonBuilder = {
            div {
                className = ClassName("h6 text-gray-800 mb-2")
                buttonBuilder()
            }
        },
    )
}

/**
 * @param title
 * @param classes
 * @param onCloseButtonPressed
 * @param bodyBuilder
 * @param buttonBuilder
 * @param buttonAfterClick
 */
fun ChildrenBuilder.modalBuilderWithClick(
    title: String,
    classes: String = "",
    onCloseButtonPressed: (() -> Unit)?,
    bodyBuilder: ChildrenBuilder.() -> Unit,
    buttonBuilder: ChildrenBuilder.() -> Unit,
    buttonAfterClick: ChildrenBuilder.() -> Unit,
) {
    div {
        className = ClassName("modal-dialog $classes")
        div {
            className = ClassName("modal-content")
            div {
                className = ClassName("modal-header")
                h5 {
                    className = ClassName("modal-title")
                    +title
                }
                onCloseButtonPressed?.let {
                    button {
                        type = ButtonType.button
                        className = ClassName("close")
                        asDynamic()["data-dismiss"] = "modal"
                        ariaLabel = "Close"
                        span {
                            fontAwesomeIcon(icon = faTimesCircle)
                            onClick = {
                                onCloseButtonPressed()
                            }
                        }
                    }
                }
            }
            className = ClassName("card card-body mt-0 p-0")
            div {
                className = ClassName("modal-body")
                bodyBuilder()
            }
            div {
                className = ClassName("d-sm-flex justify-content-center form-check")
                buttonAfterClick()
            }
            div {
                className = ClassName("d-sm-flex justify-content-center form-check")
                buttonBuilder()
            }
        }
    }
}
