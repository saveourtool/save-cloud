/**
 * Function component for project info and edit support
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.modal.defaultModalStyle
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.modal
import com.saveourtool.save.frontend.components.modal.smallTransparentModalStyle
import com.saveourtool.save.frontend.externals.modal.CssProperties
import com.saveourtool.save.frontend.externals.modal.Styles
import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import kotlinx.coroutines.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.Response
import react.*
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import kotlin.js.json

/**
 * Component that allows to change git settings in ManageGitCredentialsCard.kt
 */
val gitWindow = createGitWindow()

/**
 * GitWindow component props
 */
external interface GitWindowProps : Props {
    /**
     * Window openness
     */
    var windowOpenness: WindowOpenness

    /**
     * name of organization, assumption that it's checked by previous views and valid here
     */
    var organizationName: String

    /**
     * Git credential to update, it's null in case of creating a new one
     */
    var initialGitDto: GitDto?
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
    val fieldsWithGitInfo = props.initialGitDto.toMutableMap()

    val failedResponseWindowOpenness = useWindowOpenness()
    val (failedReason, setFailedReason) = useState<String>()
    displayModal(
        failedResponseWindowOpenness.isOpen(),
        "Error",
        failedReason?.let {
            "Failed to ${props.initialGitDto?.let { "update" } ?: "create"} git credential for ${fieldsWithGitInfo[InputTypes.GIT_URL]}: $it"
        } ?: "N/A",
        smallTransparentModalStyle,
        failedResponseWindowOpenness.closeWindowAction()
    ) {
        buttonBuilder(
            label = "Ok",
            style = "secondary",
            onClickFun = failedResponseWindowOpenness.closeWindowAction().withUnusedArg()
        )
    }

    val (_, setGitCredentialToUpsert, upsertGitCredentialRequest) =
        prepareUpsertGitCredential(props.organizationName, setFailedReason, props.windowOpenness.closeWindowAction())

    val styles = Styles(
        content = json(
            "top" to "25%",
            "left" to "35%",
            "right" to "35%",
            "bottom" to "auto",
            "overflow" to "hide",
            "z-index" to "2"
        ).unsafeCast<CssProperties>()
    )
    modal(styles) { modalProps ->
        modalProps.isOpen = props.windowOpenness.isOpen()

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
                    readOnly = props.initialGitDto != null
                    required = true
                    onChange = {
                        fieldsWithGitInfo[InputTypes.GIT_URL] = it.target.value
                    }
                    props.initialGitDto?.also {
                        asDynamic()["data-toggle"] = "tooltip"
                        asDynamic()["data-placement"] = "bottom"
                        title = "Cannot be changed on update"
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
                    setGitCredentialToUpsert(fieldsWithGitInfo.toGitDto())
                    upsertGitCredentialRequest()
                }
                val buttonName = props.initialGitDto?.let { "Update" } ?: "Create"
                +buttonName
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-outline-primary")
                onClick = props.windowOpenness.closeWindowAction().withUnusedArg()
                +"Cancel"
            }
        }
    }

    useTooltip()
}

private fun prepareUpsertGitCredential(
    organizationName: String,
    setFailedResponse: StateSetter<String?>,
    closeGitWindow: () -> Unit,
): RequestWithDependency<GitDto?> {
    val (gitCredentialToUpsert, setGitCredentialToUpsert) = useState<GitDto?>(null)
    val upsertGitCredentialRequest = useDeferredRequest {
        val response = post(
            "$apiUrl/organizations/$organizationName/upsert-git",
            headers = jsonHeaders,
            body = Json.encodeToString(requireNotNull(gitCredentialToUpsert)),
            loadingHandler = ::loadingHandler,
        )
        if (!response.ok) {
            setFailedResponse("${response.statusText} ${response.text().await()}")
        } else {
            closeGitWindow()
        }
    }
    return Triple(gitCredentialToUpsert, setGitCredentialToUpsert, upsertGitCredentialRequest)
}
