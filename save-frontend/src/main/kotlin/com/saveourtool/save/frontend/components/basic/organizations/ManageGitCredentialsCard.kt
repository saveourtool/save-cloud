/**
 * This file contains function to create ManageGitCredentialsCard
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.getHighestRole

import csstype.ClassName
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.FC
import react.Props
import react.StateSetter
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.useState

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

typealias RequestWithDependency<R> = Triple<R, StateSetter<R>, () -> Unit>

/**
 * Props for ManageGitCredentialsCard
 */
external interface ManageGitCredentialsCardProps : Props {
    /**
     * Information about user who is seeing the view
     */
    var selfUserInfo: UserInfo

    /**
     * name of organization, assumption that it's checked by previous views and valid here
     */
    var organizationName: String

    /**
     * Flag that shows if the confirm windows was shown or not
     */
    var wasConfirmationModalShown: Boolean

    /**
     * Lambda to show error after fail response
     */
    var updateErrorMessage: (Response) -> Unit

    /**
     * Lambda to show warning if current user is super admin
     */
    var showGlobalRoleWarning: () -> Unit
}

/**
 * @return ManageGitCredentialsCard
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun manageGitCredentialsCardComponent() = FC<ManageGitCredentialsCardProps> { props ->
    val (selfRole, setSelfRole) = useState(Role.NONE)
    useRequest(isDeferred = false) {
        val role = get(
            "$apiUrl/organizations/${props.organizationName}/users/roles",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<String>()
            }
            .toRole()
        if (!props.wasConfirmationModalShown && role.priority < Role.OWNER.priority && props.selfUserInfo.globalRole == Role.SUPER_ADMIN) {
            props.showGlobalRoleWarning()
        }
        setSelfRole(getHighestRole(role, props.selfUserInfo.globalRole))
    }()

    val (gitCredentials, _, fetchGitCredentialsRequest) = prepareFetchGitCredentials(props.organizationName)

    val (gitCredentialToUpsert, setGitCredentialToUpsert, upsertGitCredentialRequest) =
            prepareUpsertGitCredential(props.organizationName, props.updateErrorMessage, fetchGitCredentialsRequest)

    val (gitCredentialToDelete, setGitCredentialToDelete, deleteGitCredentialRequest) =
            prepareDeleteGitCredential(props.organizationName, props.updateErrorMessage, fetchGitCredentialsRequest)

    val (isGitWindowOpened, setGitWindowOpened) = useState(false)
    gitWindow {
        isOpen = isGitWindowOpened
        organizationName = props.organizationName
        gitDto = gitCredentialToUpsert
        onGitUpdate = {
            setGitCredentialToUpsert(it)
            upsertGitCredentialRequest()
        }
        setClosedState = {
            setGitWindowOpened(false)
        }
    }

    val (isConfirmDeleteGitCredentialWindowOpened, setConfirmDeleteGitCredentialWindowOpened) = useState(false)
    runConfirmWindowModal(
        isConfirmWindowOpen = isConfirmDeleteGitCredentialWindowOpened,
        confirmLabel = "Deletion of git credential",
        confirmMessage = "Please confirm deletion of git credential for ${gitCredentialToDelete.url}." +
                "Note, this action will also delete corresponding test suite sources.",
        okButtonLabel = "Ok",
        closeButtonLabel = "Cancel",
        handlerClose = { setConfirmDeleteGitCredentialWindowOpened(false) }) {
        // delete and close
        deleteGitCredentialRequest()
        setConfirmDeleteGitCredentialWindowOpened(false)
    }

    val canModify = selfRole == Role.SUPER_ADMIN || selfRole == Role.ADMIN
    div {
        className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0")
        gitCredentials.forEachIndexed { index, gitCredential ->
            val url = gitCredential.url
            div {
                className = ClassName("row mt-2 mr-0 justify-content-between align-items-center")
                div {
                    className = ClassName("col-7 d-flex justify-content-start align-items-center")
                    div {
                        className = ClassName("col-2 align-items-center")
                        fontAwesomeIcon(
                            when {
                                url.contains("github") -> faGithub
                                url.contains("codehub") -> faCopyright
                                else -> faHome
                            },
                            classes = "h-75 w-75"
                        )
                    }
                    div {
                        className = ClassName("col-7 text-left align-self-center pl-0")
                        +url
                    }
                }
                div {
                    className = ClassName("col-5 align-self-right d-flex align-items-center justify-content-end")
                    button {
                        className = ClassName("btn col-2 align-items-center mr-2")
                        fontAwesomeIcon(icon = faEdit)
                        id = "edit-git-credential-$index"
                        onClick = {
                            setGitCredentialToUpsert(gitCredential)
                            setGitWindowOpened(true)
                        }
                    }
                    button {
                        className = ClassName("btn col-2 align-items-center mr-2")
                        fontAwesomeIcon(icon = faTimesCircle)
                        id = "remove-git-credential-$index"
                        onClick = {
                            setGitCredentialToDelete(gitCredential)
                            setConfirmDeleteGitCredentialWindowOpened(true)
                        }
                    }
                    hidden = !canModify
                }
            }
        }
        @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
        hr {}
        div {
            className = ClassName("row d-flex justify-content-center")
            div {
                className = ClassName("col-3 d-sm-flex align-items-center justify-content-center")
                button {
                    type = ButtonType.button
                    className = ClassName("btn btn-sm btn-primary")
                    onClick = {
                        setGitCredentialToUpsert(null)
                        setGitWindowOpened(true)
                    }
                    +"Add new"
                }
            }
        }
    }

    runOnlyOnFirstRender {
        fetchGitCredentialsRequest()
    }
}

@Suppress("TYPE_ALIAS")
private fun prepareFetchGitCredentials(organizationName: String): RequestWithDependency<List<GitDto>> {
    val (gitCredentials, setGitCredentials) = useState(emptyList<GitDto>())
    val fetchGitCredentialsRequest = useRequest {
        get(
            "$apiUrl/organizations/$organizationName/list-git",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<GitDto>>()
            }.let {
                setGitCredentials(it)
            }
    }
    return Triple(gitCredentials, setGitCredentials, fetchGitCredentialsRequest)
}

private fun prepareUpsertGitCredential(
    organizationName: String,
    updateErrorMessage: (Response) -> Unit,
    fetchGitCredentialsRequest: () -> Unit
): RequestWithDependency<GitDto?> {
    val (gitCredentialToUpsert, setGitCredentialToUpsert) = useState<GitDto?>(null)
    val upsertGitCredentialRequest = useRequest(dependencies = arrayOf(gitCredentialToUpsert)) {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = post(
            "$apiUrl/organizations/$organizationName/upsert-git",
            headers = headers,
            body = Json.encodeToString(requireNotNull(gitCredentialToUpsert)),
            loadingHandler = ::loadingHandler,
        )
        if (!response.ok) {
            updateErrorMessage(response)
        } else {
            fetchGitCredentialsRequest()
        }
    }
    return Triple(gitCredentialToUpsert, setGitCredentialToUpsert, upsertGitCredentialRequest)
}

private fun prepareDeleteGitCredential(
    organizationName: String,
    updateErrorMessage: (Response) -> Unit,
    fetchGitCredentialsRequest: () -> Unit
): RequestWithDependency<GitDto> {
    val (gitCredentialToDelete, setGitCredentialToDelete) = useState(GitDto("N/A"))
    val deleteGitCredentialRequest = useRequest(dependencies = arrayOf(gitCredentialToDelete)) {
        val response = delete(
            url = "$apiUrl/organizations/$organizationName/delete-git?url=${gitCredentialToDelete.url}",
            headers = jsonHeaders,
            body = undefined,
            loadingHandler = ::loadingHandler,
        )
        if (!response.ok) {
            updateErrorMessage(response)
        } else {
            fetchGitCredentialsRequest()
        }
    }
    return Triple(gitCredentialToDelete, setGitCredentialToDelete, deleteGitCredentialRequest)
}
