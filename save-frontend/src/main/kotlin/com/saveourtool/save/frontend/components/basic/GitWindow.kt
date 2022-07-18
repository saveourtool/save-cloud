/**
 * Function component for project info and edit support
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.externals.modal.modal
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.frontend.utils.post
import com.saveourtool.save.frontend.utils.useRequest

import csstype.ClassName
import org.w3c.dom.Element
import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.dom.events.MouseEventHandler
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Component that allows to change git settings in ProjectSettingsMenu
 */
val gitWindow = gitWindow()

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
    var handlerCancel: MouseEventHandler<Element>

    /**
     * Lambda to set new git dto in state
     */
    var onGitUpdate: (GitDto) -> Unit
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
)
private fun gitWindow() = FC<RunSettingGitWindowProps> { props ->
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

    modal { modalProps ->
        modalProps.isOpen = props.isOpenGitWindow

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
