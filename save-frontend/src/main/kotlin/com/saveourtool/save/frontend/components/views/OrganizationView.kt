/**
 * A view with organization details
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.ImageInfo
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.organizations.organizationSettingsMenu
import com.saveourtool.save.frontend.components.basic.privacySpan
import com.saveourtool.save.frontend.components.basic.scoreCard
import com.saveourtool.save.frontend.components.basic.userBoard
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.http.getOrganization
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.getHighestRole
import com.saveourtool.save.v1

import csstype.*
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.xhr.FormData
import react.*
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.textarea
import react.table.columns

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.js.jso
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface OrganizationProps : PropsWithChildren {
    var organizationName: String
    var currentUserInfo: UserInfo?
}

/**
 * [State] of project view component
 */
external interface OrganizationViewState : State {
    /**
     * Flag to handle uploading a file
     */
    var isUploading: Boolean

    /**
     * Image to owner avatar
     */
    var image: ImageInfo?

    /**
     * Organization
     */
    var organization: Organization?

    /**
     * project selected menu
     */
    var selectedMenu: OrganizationMenuBar?

    /**
     * List of projects for `this` organization
     */
    var projects: Array<Project>?

    /**
     * Message of error
     */
    var errorMessage: String

    /**
     * Flag to handle error
     */
    var isErrorOpen: Boolean?

    /**
     * Error label
     */
    var errorLabel: String

    /**
     * Message of warning
     */
    var confirmMessage: String

    /**
     * State for the creation of unified confirmation logic
     */
    var confirmationType: ConfirmationType

    /**
     * Flag to handle confirm Window
     */
    var isConfirmWindowOpen: Boolean?

    /**
     * Label of confirm Window
     */
    var confirmLabel: String

    /**
     * Whether editing of organization info is disabled
     */
    var isEditDisabled: Boolean

    /**
     * Highest [Role] of a current user in organization or globally
     */
    var selfRole: Role

    /**
     * Users in organization
     */
    var usersInOrganization: List<UserInfo>?

    /**
     * Label that will be shown on Close button of modal windows
     */
    var closeButtonLabel: String?

    /**
     * Current state of description input form
     */
    var draftOrganizationDescription: String
}

/**
 * A Component for owner view
 */
class OrganizationView : AbstractView<OrganizationProps, OrganizationViewState>(false) {
    private val table = tableComponent(
        columns = columns<Project> {
            column(id = "name", header = "Evaluated Tool", { name }) { cellProps ->
                Fragment.create {
                    td {
                        a {
                            href = "#/${cellProps.row.original.organization.name}/${cellProps.value}"
                            +cellProps.value
                        }
                        privacySpan(cellProps.row.original)
                    }
                }
            }
            column(id = "description", header = "Description") {
                Fragment.create {
                    td {
                        +(it.value.description ?: "Description not provided")
                    }
                }
            }
            column(id = "rating", header = "Contest Rating") {
                Fragment.create {
                    td {
                        +"0"
                    }
                }
            }
        },
        useServerPaging = false,
        usePageSelection = false,
    )
    private lateinit var responseFromDeleteOrganization: Response

    init {
        state.isUploading = false
        state.organization = Organization("", OrganizationStatus.CREATED, null, null, null)
        state.selectedMenu = OrganizationMenuBar.INFO
        state.projects = emptyArray()
        state.closeButtonLabel = null
        state.selfRole = Role.NONE
        state.draftOrganizationDescription = ""
    }

    private fun deleteOrganization() {
        val newOrganization = state.organization
            ?.copy(status = OrganizationStatus.DELETED)
            ?.apply { id = state.organization?.id }
        setState {
            organization = newOrganization
            confirmationType = ConfirmationType.DELETE_CONFIRM
            isConfirmWindowOpen = true
            confirmLabel = ""
            confirmMessage = "Are you sure you want to delete this organization?"
        }
    }

    private fun showNotification(notificationLabel: String, notificationMessage: String) {
        setState {
            isErrorOpen = true
            errorLabel = notificationLabel
            errorMessage = notificationMessage
            closeButtonLabel = "Confirm"
        }
    }

    override fun componentDidMount() {
        super.componentDidMount()
        scope.launch {
            val avatar = getAvatar()
            val organizationLoaded = getOrganization(props.organizationName)
            val projectsLoaded = getProjectsForOrganization()
            val role = getRoleInOrganization()
            val users = getUsers()
            setState {
                image = avatar
                organization = organizationLoaded
                draftOrganizationDescription = organizationLoaded.description ?: ""
                projects = projectsLoaded
                isEditDisabled = true
                selfRole = getHighestRole(role, props.currentUserInfo?.globalRole)
                usersInOrganization = users
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
    override fun ChildrenBuilder.render() {
        runErrorModal(state.isErrorOpen, state.errorLabel, state.errorMessage, state.closeButtonLabel ?: "Close") {
            setState {
                isErrorOpen = false
                closeButtonLabel = null
            }
        }
        runConfirmWindowModal(
            state.isConfirmWindowOpen,
            state.confirmLabel,
            state.confirmMessage,
            "Ok",
            "Cancel",
            { setState { isConfirmWindowOpen = false } }) {
            when (state.confirmationType) {
                ConfirmationType.DELETE_CONFIRM -> deleteOrganizationBuilder()
                else -> throw IllegalStateException("Not implemented yet")
            }
            setState { isConfirmWindowOpen = false }
        }

        renderOrganizationMenuBar()

        when (state.selectedMenu!!) {
            OrganizationMenuBar.INFO -> renderInfo()
            OrganizationMenuBar.TOOLS -> renderTools()
            OrganizationMenuBar.SETTINGS -> renderSettings()
            else -> {
                // this is a generated else block
            }
        }
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "LongMethod",
        "ComplexMethod",
        "PARAMETER_NAME_IN_OUTER_LAMBDA",
    )
    private fun ChildrenBuilder.renderInfo() {
        // ================= Title for TOP projects ===============
        div {
            className = ClassName("row")
            div {
                className = ClassName("col-3 ml-auto")
                style = jso {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }
                h4 {
                    +"Top Tools"
                }
            }

            div {
                className = ClassName("col-3 mx-auto")
            }
        }

        // ================= Rows for TOP projects ================
        val topProjects = state.projects?.sortedByDescending { it.contestRating }?.take(TOP_PROJECTS_NUMBER)

        div {
            className = ClassName("row")
            style = jso {
                justifyContent = JustifyContent.center
            }
            renderTopProject(topProjects?.getOrNull(0))
            renderTopProject(topProjects?.getOrNull(1))
        }

        @Suppress("MAGIC_NUMBER")
        div {
            className = ClassName("row")
            style = jso<CSSProperties> {
                justifyContent = JustifyContent.center
            }
            renderTopProject(topProjects?.getOrNull(2))
            renderTopProject(topProjects?.getOrNull(3))
        }

        div {
            className = ClassName("row")
            style = jso {
                justifyContent = JustifyContent.center
            }
            div {
                className = ClassName("col-3 mb-4")
                div {
                    className = ClassName("card shadow mb-4")
                    div {
                        className = ClassName("card-header py-3")
                        div {
                            className = ClassName("row")
                            h6 {
                                className = ClassName("m-0 font-weight-bold text-primary")
                                style = jso {
                                    display = Display.flex
                                    alignItems = AlignItems.center
                                }
                                +"Description"
                            }
                            if (state.selfRole.hasWritePermission() && state.isEditDisabled) {
                                button {
                                    className = ClassName("btn btn-link text-xs text-muted text-left ml-auto")
                                    +"Edit  "
                                    fontAwesomeIcon(icon = faEdit)
                                    onClick = {
                                        turnEditMode(isOff = false)
                                    }
                                }
                            }
                        }
                    }
                    div {
                        className = ClassName("card-body")
                        textarea {
                            className = ClassName("auto_height form-control-plaintext pt-0 pb-0")
                            value = state.draftOrganizationDescription
                            disabled = !state.selfRole.hasWritePermission() || state.isEditDisabled
                            onChange = {
                                setNewDescription(it.target.value)
                            }
                        }
                    }
                    div {
                        className = ClassName("ml-3 mt-2 align-items-right float-right")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faCheck)
                            hidden = !state.selfRole.hasWritePermission() || state.isEditDisabled
                            onClick = {
                                state.organization?.let { onOrganizationSave(it) }
                                turnEditMode(true)
                            }
                        }

                        button {
                            type = ButtonType.button
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faTimesCircle)
                            hidden = !state.selfRole.hasWritePermission() || state.isEditDisabled
                            onClick = {
                                turnEditMode(true)
                            }
                        }
                    }
                }
            }

            div {
                className = ClassName("col-3")
                userBoard {
                    users = state.usersInOrganization.orEmpty()
                }
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    private fun ChildrenBuilder.renderTools() {
        div {
            className = ClassName("row justify-content-center")
            // ===================== RIGHT COLUMN =======================================================================
            div {
                className = ClassName("col-6")
                div {
                    className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Projects"
                }

                table {
                    getData = { _, _ ->
                        getProjectsFromCache()
                    }
                    getPageCount = null
                }
            }
        }
    }

    private fun setNewDescription(value: String) {
        setState {
            draftOrganizationDescription = value
        }
    }

    private fun onOrganizationSave(newOrganization: Organization) {
        newOrganization.apply {
            description = state.draftOrganizationDescription
        }
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            val response = post(
                "$apiUrl/organization/${props.organizationName}/update",
                headers,
                Json.encodeToString(newOrganization),
                loadingHandler = ::noopLoadingHandler,
            )
            if (response.ok) {
                setState {
                    organization = newOrganization
                }
            }
        }
    }

    private fun ChildrenBuilder.renderSettings() {
        organizationSettingsMenu {
            organizationName = props.organizationName
            currentUserInfo = props.currentUserInfo ?: UserInfo("Undefined")
            selfRole = state.selfRole
            deleteOrganizationCallback = {
                if (state.projects?.size != 0) {
                    setState {
                        isErrorOpen = true
                        errorLabel = ""
                        errorMessage = "You cannot delete an organization because there are projects connected to it. " +
                                "Delete all the projects and try again."
                    }
                } else {
                    deleteOrganization()
                }
            }
            updateErrorMessage = {
                setState {
                    isErrorOpen = true
                    errorLabel = ""
                    errorMessage = "Failed to save organization info: ${it.status} ${it.statusText}"
                }
            }
            updateNotificationMessage = ::showNotification
        }
    }

    private fun turnEditMode(isOff: Boolean) {
        setState {
            isEditDisabled = isOff
        }
    }

    /**
     * Small workaround to avoid the request to the backend for the second time and to use it inside the Table view
     */
    private fun getProjectsFromCache(): Array<Project> = state.projects ?: emptyArray()

    private suspend fun getProjectsForOrganization(): Array<Project> = get(
        url = "$apiUrl/projects/get/not-deleted-projects-by-organization?organizationName=${props.organizationName}",
        headers = Headers().also {
            it.set("Accept", "application/json")
        },
        loadingHandler = ::classLoadingHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString()
        }

    private suspend fun getRoleInOrganization(): Role = get(
        url = "$apiUrl/organizations/${props.organizationName}/users/roles",
        headers = Headers().also {
            it.set("Accept", "application/json")
        },
        loadingHandler = ::classLoadingHandler,
        responseHandler = ::noopResponseHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString()
        }

    private suspend fun getUsers(): List<UserInfo> = get(
        url = "$apiUrl/organizations/${props.organizationName}/users",
        headers = Headers().also {
            it.set("Accept", "application/json")
        },
        loadingHandler = ::classLoadingHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString()
        }

    private fun postImageUpload(element: HTMLInputElement) =
            scope.launch {
                setState {
                    isUploading = true
                }
                element.files!!.asList().single().let { file ->
                    val response: ImageInfo? = post(
                        "$apiUrl/image/upload?owner=${props.organizationName}&type=${AvatarType.ORGANIZATION}",
                        Headers(),
                        FormData().apply {
                            append("file", file)
                        },
                        loadingHandler = ::noopLoadingHandler,
                    )
                        .decodeFromJsonString()
                    setState {
                        image = response
                    }
                }
                setState {
                    isUploading = false
                }
            }

    private suspend fun getAvatar() = get(
        "$apiUrl/organization/${props.organizationName}/avatar",
        Headers(),
        loadingHandler = ::noopLoadingHandler,
    ).unsafeMap {
        it.decodeFromJsonString<ImageInfo>()
    }

    private fun ChildrenBuilder.renderTopProject(topProject: Project?) {
        topProject ?: return
        div {
            className = ClassName("col-3 mb-4")
            scoreCard {
                name = topProject.name
                contestScore = topProject.contestRating.toDouble()
            }
        }
    }

    @Suppress("LongMethod", "TOO_LONG_FUNCTION", "MAGIC_NUMBER")
    private fun ChildrenBuilder.renderOrganizationMenuBar() {
        div {
            className = ClassName("row")
            div {
                className = ClassName("col-3 ml-auto")
                style = jso {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }
                label {
                    input {
                        type = InputType.file
                        hidden = true
                        onChange = { event ->
                            postImageUpload(event.target)
                        }
                    }
                    ariaLabel = "Change organization's avatar"
                    img {
                        className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                        src = state.image?.path?.let {
                            "/api/$v1/avatar$it"
                        }
                            ?: run {
                                "img/company.svg"
                            }
                        height = 100.0
                        width = 100.0
                    }
                }

                h1 {
                    className = ClassName("h3 mb-0 text-gray-800 ml-2")
                    +(state.organization?.name ?: "N/A")
                }
            }

            div {
                className = ClassName("col-3 mx-0")
                style = jso {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }

                nav {
                    className = ClassName("nav nav-tabs")
                    OrganizationMenuBar.values()
                        .filter { it != OrganizationMenuBar.SETTINGS || state.selfRole.isHigherOrEqualThan(Role.ADMIN) }
                        .forEachIndexed { i, projectMenu ->
                            li {
                                className = ClassName("nav-item")
                                val classVal = if ((i == 0 && state.selectedMenu == null) || state.selectedMenu == projectMenu) {
                                    " active font-weight-bold"
                                } else {
                                    ""
                                }
                                p {
                                    className = ClassName("nav-link $classVal text-gray-800")
                                    onClick = {
                                        if (state.selectedMenu != projectMenu) {
                                            setState {
                                                selectedMenu = projectMenu
                                            }
                                        }
                                    }
                                    +projectMenu.name
                                }
                            }
                        }
                }
            }

            div {
                className = ClassName("col-3 mr-auto")
                style = jso {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }

                if (state.selfRole.isHigherOrEqualThan(Role.ADMIN)) {
                    button {
                        type = ButtonType.button
                        className = ClassName("btn btn-primary")
                        a {
                            className = ClassName("text-light")
                            href = "#/creation/"
                            +"+ New Tool"
                        }
                    }
                }
            }
        }
    }

    private fun deleteOrganizationBuilder() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            responseFromDeleteOrganization =
                    delete(
                        "$apiUrl/organization/${props.organizationName}/delete",
                        headers,
                        body = undefined,
                        loadingHandler = ::noopLoadingHandler,
                    )
        }.invokeOnCompletion {
            if (responseFromDeleteOrganization.ok) {
                window.location.href = "${window.location.origin}/"
            }
        }
    }

    companion object :
        RStatics<OrganizationProps, OrganizationViewState, OrganizationView, Context<RequestStatusContext>>(
        OrganizationView::class
    ) {
        const val TOP_PROJECTS_NUMBER = 4
        init {
            contextType = requestStatusContext
        }
    }
}
