/**
 * Function component for project info and edit support
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.modal.modal

import csstype.ClassName
import react.*
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input

/**
 * Component that allows to change git settings in ManageGitCredentialsCard.kt
 */
val gitWindow = createGitWindow()

/**
 * RunSettingGitWindow component props
 */
external interface GitWindowProps : Props {
    /**
     * Flag to open window
     */
    var isOpen: Boolean

    /**
     * name of organization, assumption that it's checked by previous views and valid here
     */
    var organizationName: String

    /**
     * Git credential to update, it's null in case of creating a new one
     */
    var gitDto: GitDto?

    /**
     * Lambda to upsert git dto
     */
    var onGitUpdate: (GitDto) -> Unit

    /**
     * Lambda to set state about current modal window to closed
     */
    var setClosedState: () -> Unit
}

private fun GitDto?.toMutableMap(): MutableMap<InputTypes, String> = mutableMapOf<InputTypes, String>().also {
    it[InputTypes.GIT_URL] = this?.url ?: ""
    it[InputTypes.GIT_USER] = this?.username ?: ""
    it[InputTypes.GIT_TOKEN] = this?.password ?: ""
}

private fun MutableMap<InputTypes, String>.toGitDto(): GitDto = GitDto(
    url = getValue(InputTypes.GIT_URL),
    username = getValue(InputTypes.GIT_USER),
    password = getValue(InputTypes.GIT_TOKEN),
)

@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
)
private fun createGitWindow() = FC<GitWindowProps> { props ->
    val fieldsWithGitInfo = props.gitDto.toMutableMap()

    modal { modalProps ->
        modalProps.isOpen = props.isOpen

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
                    defaultValue = fieldsWithGitInfo[InputTypes.GIT_URL]
                    readOnly = props.gitDto != null
                    required = true
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
                +"Git Username:"
            }
            div {
                className = ClassName("col-7 input-group pl-0")
                input {
                    type = InputType.text
                    className = ClassName("form-control")
                    defaultValue = fieldsWithGitInfo[InputTypes.GIT_USER]
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
                    props.setClosedState()
                    props.onGitUpdate(fieldsWithGitInfo.toGitDto())
                }
                val buttonName = props.gitDto?.let { "Save" } ?: "Create"
                +buttonName
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-outline-primary")
                onClick = {
                    props.setClosedState()
                }
                +"Cancel"
            }
        }
    }
}
