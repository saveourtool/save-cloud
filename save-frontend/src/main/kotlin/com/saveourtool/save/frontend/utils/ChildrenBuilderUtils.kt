/**
 * Utilities for kotlin-js RBuilder
 */

@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.components.basic.InputTypes
import com.saveourtool.save.frontend.externals.modal.modal
import csstype.ClassName

import org.w3c.fetch.Headers

import react.dom.html.ButtonType
import react.dom.html.InputType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLButtonElement
import react.*
import react.dom.events.MouseEventHandler
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.input

/**
 * Enum that stores types of confirmation windows for different situations.
 */
enum class ConfirmationType {
    DELETE_CONFIRM,
    NO_BINARY_CONFIRM,
    NO_CONFIRM,
    ;
}

/**
 * RunSettingGitWindow component props
 */
external interface RunSettingGitWindowProps : Props {
    /**
     * Flag to open window
     */
    var isOpenGitWindow: Boolean?

    /**
     * Project
     */
    var project: Project?

    /**
     * Git info for this project
     */
    var gitDto: GitDto?

    /**
     * Lambda to close window
     */
    var handlerCancel: MouseEventHandler<HTMLButtonElement>

    /**
     * Lambda to set new git dto in state
     */
    var onGitUpdate: (GitDto) -> Unit
}

/**
 * @param isErrorOpen flag to handle error
 * @param errorLabel label of error
 * @param errorMessage message of error
 * @param closeButtonLabel label that will be shown on the only button
 * @param handler handler to close
 * @return modal
 */
fun ChildrenBuilder.runErrorModal(
    isErrorOpen: Boolean?,
    errorLabel: String,
    errorMessage: String,
    closeButtonLabel: String = "Close",
    handler: MouseEventHandler<HTMLButtonElement>
) = modal {
    it.isOpen = isErrorOpen
    it.contentLabel = errorLabel
    div {
        className = ClassName("row align-items-center justify-content-center")
        h2 {
            className = ClassName("h6 text-gray-800")
            +(errorMessage)
        }
    }
    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")
        button {
            type = ButtonType.button
            className = ClassName("btn btn-primary")
            onClick = handler
            +closeButtonLabel
        }
    }
}

/**
 * @param isConfirmWindowOpen flag to handle confirm Window
 * @param confirmLabel label of confirm Window
 * @param confirmMessage message
 * @param okButtonLabel label for ok button
 * @param closeButtonLabel label for close button
 * @param handlerClose handler to close
 * @param handler handler to event and close
 * @return modal
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.runConfirmWindowModal(
    isConfirmWindowOpen: Boolean?,
    confirmLabel: String,
    confirmMessage: String,
    okButtonLabel: String = "Ok",
    closeButtonLabel: String = "Close",
    handlerClose: MouseEventHandler<HTMLButtonElement>,
    handler: MouseEventHandler<HTMLButtonElement>
) = modal {
    it.isOpen = isConfirmWindowOpen
    it.contentLabel = confirmLabel
    div {
        className = ClassName("row align-items-center justify-content-center")
        h2 {
            className = ClassName("h6 text-gray-800 mb-2")
            +(confirmMessage)
        }
    }
    div {
        className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")
        button {
            type = ButtonType.button
            className = ClassName("btn btn-primary mr-3")
            onClick = handler
            +okButtonLabel
        }
        button {
            type = ButtonType.button
            className = ClassName("btn btn-outline-primary")
            onClick = handlerClose
            +closeButtonLabel
        }
    }
}

/**
 * @return a function component
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
)
fun runSettingGitWindow() = FC<RunSettingGitWindowProps> { props ->
    val (fieldsWithGitInfo, setFieldsWithGitInfo) = useState(mutableMapOf<InputTypes, String>())

    val updateGit = useRequest(arrayOf(props.project), isDeferred = true) {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }

        val git = GitDto(
            fieldsWithGitInfo[InputTypes.GIT_URL]?.trim() ?: props.gitDto!!.url,
            fieldsWithGitInfo[InputTypes.GIT_USER]?.trim() ?: props.gitDto?.username,
            fieldsWithGitInfo[InputTypes.GIT_TOKEN]?.trim() ?: props.gitDto?.password,
            props.gitDto?.branch,
            props.gitDto?.hash,
        )

        val response = post(
            "$apiUrl/projects/update/git?projectId=${props.project?.id}",
            headers,
            Json.encodeToString(git),
            loadingHandler = ::noopLoadingHandler,
        )

        if (response.ok) {
            props.onGitUpdate(git)
        }
    }

    modal {
        it.isOpen = props.isOpenGitWindow

        div {
            className = ClassName("row mt-2 ml-2 mr-2")
            div {
                className = ClassName("col-5 text-left align-self-center")
                +"Git Username:"
            }
            div {
                className = ClassName("col-7 input-group pl-0")
                input {
                    type = InputType.text
                    className = ClassName("form-control")
                    defaultValue = props.gitDto?.username ?: ""
                    onChange = {
                        fieldsWithGitInfo[InputTypes.GIT_USER] = it.target.value
                    }
                }
            }
        }
        div {
            className = ClassName("row mt-2 ml-2 mr-2")
            div {
                className = ClassName("col-5 text-left align-self-center")
                +"Git Url:"
            }
            div {
                className = ClassName("col-7 input-group pl-0")
                input {
                    type = InputType.text
                    className = ClassName("form-control")
                    defaultValue = props.gitDto?.url ?: ""
                    onChange = {
                        fieldsWithGitInfo[InputTypes.GIT_URL] = it.target.value
                    }
                }
            }
        }
        div {
            className = ClassName("row mt-2 ml-2 mr-2")
            div {
                className = ClassName("col-5 text-left align-self-center")
                +"Git Token:"
            }
            div {
                className = ClassName("col-7 input-group pl-0")
                input {
                    type = InputType.text
                    className = ClassName("form-control")
                    onChange = {
                        fieldsWithGitInfo[InputTypes.GIT_TOKEN] = it.target.value
                    }
                }
            }
        }
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")
            button {
                type = ButtonType.button
                className = ClassName("btn btn-primary mr-3")
                onClick = {
                    setFieldsWithGitInfo(fieldsWithGitInfo)
                    updateGit()
                }
                +"Save"
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-outline-primary")
                onClick = props.handlerCancel
                +"Cancel"
            }
        }
    }
}
