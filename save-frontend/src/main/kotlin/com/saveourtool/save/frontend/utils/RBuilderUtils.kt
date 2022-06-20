/**
 * Utilities for kotlin-js RBuilder
 */

@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.components.basic.InputTypes
import com.saveourtool.save.frontend.externals.modal.modal

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.fetch.Headers
import react.Props
import react.RBuilder
import react.dom.*
import react.fc
import react.useState

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
}

/**
 * @param isErrorOpen flag to handle error
 * @param errorLabel label of error
 * @param errorMessage message of error
 * @param closeButtonLabel label that will be shown on the only button
 * @param handler handler to close
 * @return modal
 */
fun RBuilder.runErrorModal(
    isErrorOpen: Boolean?,
    errorLabel: String,
    errorMessage: String,
    closeButtonLabel: String = "Close",
    handler: (Event) -> Unit
) = modal {
    attrs {
        isOpen = isErrorOpen
        contentLabel = errorLabel
    }
    div("row align-items-center justify-content-center") {
        h2("h6 text-gray-800") {
            +(errorMessage)
        }
    }
    div("d-sm-flex align-items-center justify-content-center mt-4") {
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = handler
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
fun RBuilder.runConfirmWindowModal(
    isConfirmWindowOpen: Boolean?,
    confirmLabel: String,
    confirmMessage: String,
    okButtonLabel: String = "Ok",
    closeButtonLabel: String = "Close",
    handlerClose: (Event) -> Unit,
    handler: (Event) -> Unit
) = modal {
    attrs {
        isOpen = isConfirmWindowOpen
        contentLabel = confirmLabel
    }
    div("row align-items-center justify-content-center") {
        h2("h6 text-gray-800 mb-2") {
            +(confirmMessage)
        }
    }
    div("d-sm-flex align-items-center justify-content-center mt-4") {
        button(type = ButtonType.button, classes = "btn btn-primary mr-3") {
            attrs.onClickFunction = handler
            +okButtonLabel
        }
        button(type = ButtonType.button, classes = "btn btn-outline-primary") {
            attrs.onClickFunction = handlerClose
            +closeButtonLabel
        }
    }
}

/**
 * @param handlerCancel
 * @param handler
 * @return a function component
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
)
fun runSettingGitWindow(
    handlerCancel: (Event) -> Unit,
    handler: (GitDto) -> Unit,
) = fc<RunSettingGitWindowProps> { props ->
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
            handler(git)
        }
    }

    modal {
        attrs {
            isOpen = props.isOpenGitWindow
        }

        div("row mt-2 ml-2 mr-2") {
            div("col-5 text-left align-self-center") {
                +"Git Username:"
            }
            div("col-7 input-group pl-0") {
                input(type = InputType.text) {
                    attrs["class"] = "form-control"
                    attrs {
                        defaultValue = props.gitDto?.username ?: ""
                        onChange = {
                            fieldsWithGitInfo[InputTypes.GIT_USER] = (it.target as HTMLInputElement).value
                        }
                    }
                }
            }
        }
        div("row mt-2 ml-2 mr-2") {
            div("col-5 text-left align-self-center") {
                +"Git Url:"
            }
            div("col-7 input-group pl-0") {
                input(type = InputType.text) {
                    attrs["class"] = "form-control"
                    attrs {
                        defaultValue = props.gitDto?.url ?: ""
                        onChange = {
                            fieldsWithGitInfo[InputTypes.GIT_URL] = (it.target as HTMLInputElement).value
                        }
                    }
                }
            }
        }
        div("row mt-2 ml-2 mr-2") {
            div("col-5 text-left align-self-center") {
                +"Git Token:"
            }
            div("col-7 input-group pl-0") {
                input(type = InputType.text) {
                    attrs["class"] = "form-control"
                    attrs {
                        onChange = {
                            fieldsWithGitInfo[InputTypes.GIT_TOKEN] = (it.target as HTMLInputElement).value
                        }
                    }
                }
            }
        }
        div("d-sm-flex align-items-center justify-content-center mt-4") {
            button(type = ButtonType.button, classes = "btn btn-primary mr-3") {
                attrs.onClickFunction = {
                    setFieldsWithGitInfo(fieldsWithGitInfo)
                    updateGit()
                }
                +"Save"
            }
            button(type = ButtonType.button, classes = "btn btn-outline-primary") {
                attrs.onClickFunction = handlerCancel
                +"Cancel"
            }
        }
    }
}
