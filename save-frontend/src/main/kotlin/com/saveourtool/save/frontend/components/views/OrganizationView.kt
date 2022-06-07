/**
 * A view with organization details
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.ImageInfo
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.moreOrEqualThan
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.organizationSettingsMenu
import com.saveourtool.save.frontend.components.basic.privacySpan
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
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.xhr.FormData
import react.*
import react.dom.*
import react.table.columns

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.hidden
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
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
    var isEditDisabled: Boolean?

    /**
     * Role of user is viewer
     */
    var isRoleViewer: Boolean?

    /**
     * Users in organization
     */
    var usersInOrganization: List<UserInfo>?

    /**
     * Label that will be shown on Close button of modal windows
     */
    var closeButtonLabel: String?
}

/**
 * A Component for owner view
 */
class OrganizationView : AbstractView<OrganizationProps, OrganizationViewState>(false) {
    private val organizationSettingsMenu = organizationSettingsMenu(
        deleteOrganizationCallback = {
            if (state.projects?.size != 0) {
                setState {
                    isErrorOpen = true
                    errorLabel = ""
                    errorMessage = "You cannot delete an organization because there are projects connected to it." +
                            "Delete all the projects and try again."
                }
            } else {
                deleteOrganization()
            }
        },
        updateErrorMessage = {
            setState {
                isErrorOpen = true
                errorLabel = ""
                errorMessage = "Failed to save organization info: ${it.status} ${it.statusText}"
            }
        },
        updateNotificationMessage = ::showNotification
    )
    private var descriptionTmp: String = ""
    private lateinit var responseFromDeleteOrganization: Response

    init {
        state.isUploading = false
        state.organization = Organization("", OrganizationStatus.CREATED, null, null, null)
        state.selectedMenu = OrganizationMenuBar.INFO
        state.projects = emptyArray()
        state.closeButtonLabel = null
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
            val role = getHighestRole(getRoleInOrganization(), props.currentUserInfo?.globalRole)
            val users = getUsers()
            setState {
                image = avatar
                organization = organizationLoaded
                projects = projectsLoaded
                isEditDisabled = role.moreOrEqualThan(Role.ADMIN)
                isRoleViewer = !role.moreOrEqualThan(Role.ADMIN)
                usersInOrganization = users
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
    override fun RBuilder.render() {
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
    private fun RBuilder.renderInfo() {
        // ================= Title for TOP projects ===============
        div("row") {
            div("col-3 ml-auto") {
                attrs["style"] = jso<CSSProperties> {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }
                h4 {
                    +"Top Tools"
                }
            }

            div("col-3 mx-auto") {
                }
        }

        // ================= Rows for TOP projects ================
        val topProjects = state.projects?.sortedByDescending { it.contestRating }?.take(TOP_PROJECTS_NUMBER)

        div("row") {
            attrs["style"] = jso<CSSProperties> {
                justifyContent = JustifyContent.center
            }
            renderTopProject(topProjects?.getOrNull(0))
            renderTopProject(topProjects?.getOrNull(1))
        }

        @Suppress("MAGIC_NUMBER")
        div("row") {
            attrs["style"] = jso<CSSProperties> {
                justifyContent = JustifyContent.center
            }
            renderTopProject(topProjects?.getOrNull(2))
            renderTopProject(topProjects?.getOrNull(3))
        }

        div("row") {
            attrs["style"] = jso<CSSProperties> {
                justifyContent = JustifyContent.center
            }
            div("col-3 mb-4") {
                div("card shadow mb-4") {
                    div("card-header py-3") {
                        div("row") {
                            h6("m-0 font-weight-bold text-primary") {
                                attrs["style"] = jso<CSSProperties> {
                                    display = Display.flex
                                    alignItems = AlignItems.center
                                }
                                +"Description"
                            }
                            if (state.isRoleViewer == false && state.isEditDisabled == true) {
                                button(classes = "btn btn-link text-xs text-muted text-left ml-auto") {
                                    +"Edit  "
                                    fontAwesomeIcon(icon = faEdit)
                                    attrs.onClickFunction = {
                                        turnEditMode(isOff = false)
                                    }
                                }
                            }
                        }
                    }
                    div("card-body") {
                        textarea {
                            attrs["class"] = "auto_height form-control-plaintext pt-0 pb-0"
                            if (state.isEditDisabled != false) {
                                attrs.value = state.organization?.description ?: ""
                            }
                            attrs.disabled = state.isRoleViewer == true || (state.isEditDisabled ?: true)
                            attrs.onChange = { event ->
                                val tg = event.target as HTMLTextAreaElement
                                setNewDescription(tg.value)
                            }
                        }
                    }
                    div("ml-3 mt-2 align-items-right float-right") {
                        button(type = ButtonType.button, classes = "btn") {
                            fontAwesomeIcon(icon = faCheck)
                            attrs.hidden = state.isRoleViewer == true || (state.isEditDisabled ?: true)
                            attrs.onClick = {
                                state.organization?.let { onOrganizationSave(it) }
                                turnEditMode(true)
                            }
                        }

                        button(type = ButtonType.button, classes = "btn") {
                            fontAwesomeIcon(icon = faTimesCircle)
                            attrs.hidden = state.isRoleViewer == true || (state.isEditDisabled ?: true)
                            attrs.onClick = {
                                turnEditMode(true)
                            }
                        }
                    }
                }
            }

            div("col-3") {
                div("latest-photos") {
                    div("row") {
                        state.usersInOrganization?.forEach {
                            div("col-md-4") {
                                figure {
                                    img(classes = "img-fluid") {
                                        attrs["src"] = it.avatar?.let { path ->
                                            "/api/$v1/avatar$path"
                                        }
                                            ?: run {
                                                "img/user.svg"
                                            }
                                        attrs["alt"] = ""
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val table = tableComponent(
        columns = columns<Project> {
            column(id = "name", header = "Evaluated Tool", { name }) { cellProps ->
                buildElement {
                    td {
                        a(href = "#/${cellProps.row.original.organization.name}/${cellProps.value}") { +cellProps.value }
                        privacySpan(cellProps.row.original)
                    }
                }
            }
            column(id = "description", header = "Description") {
                buildElement {
                    td {
                        +(it.value.description ?: "Description not provided")
                    }
                }
            }
            column(id = "rating", header = "Contest Rating") {
                buildElement {
                    td {
                        +"0"
                    }
                }
            }
        },
        useServerPaging = false,
        usePageSelection = false,
    )

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    private fun RBuilder.renderTools() {
        div("row justify-content-center") {
            // ===================== RIGHT COLUMN =======================================================================
            div("col-6") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Projects"
                }

                child(table) {
                    attrs.getData = { _, _ ->
                        getProjectsFromCache()
                    }
                }
            }
        }
    }

    private fun setNewDescription(value: String) {
        descriptionTmp = value
    }

    private fun onOrganizationSave(newOrganization: Organization) {
        newOrganization.apply {
            description = descriptionTmp
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
                loadingHandler = ::loadingHandler,
            )
            if (response.ok) {
                setState {
                    organization = newOrganization
                }
            }
        }
    }

    private fun RBuilder.renderSettings() {
        child(organizationSettingsMenu) {
            attrs.organizationName = props.organizationName
            attrs.currentUserInfo = props.currentUserInfo ?: UserInfo("Undefined")
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
        ::loadingHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString()
        }

    private suspend fun getRoleInOrganization(): Role = get(
        url = "$apiUrl/organizations/${props.organizationName}/users/roles",
        headers = Headers().also {
            it.set("Accept", "application/json")
        },
        ::loadingHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString()
        }

    private suspend fun getUsers(): List<UserInfo> = get(
        url = "$apiUrl/organizations/${props.organizationName}/users",
        headers = Headers().also {
            it.set("Accept", "application/json")
        },
        ::loadingHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString<List<UserInfo>>()
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
        "$apiUrl/organization/${props.organizationName}/avatar", Headers(), ::noopLoadingHandler
    ).unsafeMap {
        it.decodeFromJsonString<ImageInfo>()
    }

    private fun RBuilder.renderTopProject(topProject: Project?) {
        topProject ?: return

        div("col-3 mb-4") {
            div("card border-left-info shadow h-70 py-2") {
                div("card-body") {
                    div("row no-gutters align-items-center") {
                        div("col mr-2") {
                            renderHeaderOfTopProject(topProject)
                            renderTopProjectProgressBar(topProject)
                        }
                    }
                }
            }
        }
    }

    private fun RBuilder.renderTopProjectProgressBar(topProject: Project) {
        div("row no-gutters align-items-center") {
            div("col-auto") {
                div("h5 mb-0 mr-3 font-weight-bold text-gray-800") {
                    +"${topProject.contestRating}"
                }
            }
            div("col") {
                div("progress progress-sm mr-2") {
                    div("progress-bar bg-info") {
                        attrs["role"] = "progressbar"
                        attrs["style"] = jso<CSSProperties> {
                            width = "${topProject.contestRating}%".unsafeCast<Width>()
                        }
                        attrs["aria-valuenow"] = "100"
                        attrs["aria-valuemin"] = "0"
                        attrs["aria-valuemax"] = "100"
                    }
                }
            }
        }
    }

    private fun RBuilder.renderHeaderOfTopProject(topProject: Project) {
        div("row") {
            attrs["style"] = jso<CSSProperties> {
                justifyContent = JustifyContent.center
                display = Display.flex
                alignItems = AlignItems.center
            }
            div("text-xs font-weight-bold text-info text-uppercase mb-1 ml-2") {
                attrs["style"] = jso<CSSProperties> {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }

                +"Rating"
            }
            div("col") {
                h6 {
                    attrs["style"] = jso<CSSProperties> {
                        justifyContent = JustifyContent.center
                        display = Display.flex
                        alignItems = AlignItems.center
                    }
                    +topProject.name
                }
            }
        }
    }

    @Suppress("LongMethod", "TOO_LONG_FUNCTION")
    private fun RBuilder.renderOrganizationMenuBar() {
        div("row") {
            div("col-3 ml-auto") {
                attrs["style"] = jso<CSSProperties> {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }
                label {
                    input(type = InputType.file) {
                        attrs.hidden = true
                        attrs {
                            onChangeFunction = { event ->
                                val target = event.target as HTMLInputElement
                                postImageUpload(target)
                            }
                        }
                    }
                    attrs["aria-label"] = "Change organization's avatar"
                    img(classes = "avatar avatar-user width-full border color-bg-default rounded-circle") {
                        attrs.src = state.image?.path?.let {
                            "/api/$v1/avatar$it"
                        }
                            ?: run {
                                "img/company.svg"
                            }
                        attrs.height = "100"
                        attrs.width = "100"
                    }
                }

                h1("h3 mb-0 text-gray-800 ml-2") {
                    +(state.organization?.name ?: "N/A")
                }
            }

            div("col-3 mx-0") {
                attrs["style"] = jso<CSSProperties> {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }

                nav("nav nav-tabs") {
                    OrganizationMenuBar.values().forEachIndexed { i, projectMenu ->
                        li("nav-item") {
                            val classVal =
                                    if ((i == 0 && state.selectedMenu == null) || state.selectedMenu == projectMenu) " active font-weight-bold" else ""
                            p("nav-link $classVal text-gray-800") {
                                attrs.onClickFunction = {
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

            div("col-3 mr-auto") {
                attrs["style"] = jso<CSSProperties> {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }

                button(type = ButtonType.button, classes = "btn btn-primary") {
                    a(classes = "text-light", href = "#/creation/") {
                        +"+ New Tool"
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
                    delete("$apiUrl/organization/${props.organizationName}/delete", headers, body = undefined, ::noopLoadingHandler)
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
