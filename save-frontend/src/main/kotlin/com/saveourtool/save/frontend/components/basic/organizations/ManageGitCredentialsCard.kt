package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.getHighestRole
import csstype.ClassName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.FC
import react.Props
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.useState

external interface ManageGitCredentialsCardProps: Props {
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
     * Lambda to get users from project/organization
     */
    var getUserGroups: (UserInfo) -> Map<String, Role>

    /**
     * Lambda to show warning if current user is super admin
     */
    var showGlobalRoleWarning: () -> Unit
}

fun manageGitCredentialsCardComponent() = FC<ManageGitCredentialsCardProps> { props ->
    val (selfRole, setSelfRole) = useState(Role.NONE)
    useRequest(isDeferred = false) {
        val role = get(
            "$apiUrl/organizations/${props.organizationName}/users/roles",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
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

    val (gitCredentials, setGitCredentials) = useState(emptyList<GitDto>())
    val fetchGitCredentials = useRequest {
        get(
            "$apiUrl/organizations/${props.organizationName}/list-git",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<GitDto>>()
            }.let {
                setGitCredentials(it)
            }
    }

    val (gitCredentialToUpsert, setGitCredentialToUpsert) = useState<GitDto?>(null)
    val upsertGitCredential = useRequest(dependencies = arrayOf(gitCredentialToUpsert)) {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = post(
            "$apiUrl/organizations/${props.organizationName}/upsert-git",
            headers = headers,
            body = Json.encodeToString(requireNotNull(gitCredentialToUpsert)),
            loadingHandler = ::loadingHandler,
        )
        if (!response.ok) {
            props.updateErrorMessage(response)
        } else {
            fetchGitCredentials()
        }
    }

    val (gitCredentialToDelete, setGitCredentialToDelete) = useState(GitDto("https://github.com/"))
    val deleteGitCredential = useRequest(dependencies = arrayOf(gitCredentialToDelete)) {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = delete(
            url = "$apiUrl/organizations/${props.organizationName}/delete-git?url=${gitCredentialToDelete.url}",
            headers = headers,
            body = undefined,
            loadingHandler = ::loadingHandler,
        )
        if (!response.ok) {
            props.updateErrorMessage(response)
        } else {
            fetchGitCredentials()
        }
    }

    val (isGitWindowOpened, setGitWindowOpened) = useState(false)
    gitWindow {
        isOpen = isGitWindowOpened
        organizationName = props.organizationName
        gitDto = gitCredentialToUpsert
        onGitUpdate = {
            setGitCredentialToUpsert(it)
            upsertGitCredential()
        }
        setClosedState = {
            setGitWindowOpened(false)
        }
    }

    val (isConfirmDeleteGitCredentialWindowOpened, setConfirmDeleteGitCredentialWindowOpened) = useState(false)
    runConfirmWindowModal(
        isConfirmWindowOpen = isConfirmDeleteGitCredentialWindowOpened,
        confirmLabel = "Deletion of git credential",
        confirmMessage = "Please confirm deletion of git credential for ${gitCredentialToDelete.url}",
        okButtonLabel = "Ok",
        closeButtonLabel = "Cancel",
        handlerClose = { setConfirmDeleteGitCredentialWindowOpened(false) }) {
        // delete and close
        deleteGitCredential()
        setConfirmDeleteGitCredentialWindowOpened(false)
    }

    val canModify = selfRole == Role.SUPER_ADMIN || selfRole == Role.ADMIN
    div {
        className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0")
        for (gitCredential in gitCredentials) {
            val url = gitCredential.url
            val index = gitCredentials.indexOf(gitCredential)
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

    val (isFirstRender, setFirstRender) = useState(true)
    if (isFirstRender) {
        fetchGitCredentials()
        setFirstRender(false)
    }
}
