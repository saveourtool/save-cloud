/**
 * A view with organization details
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.ImageInfo
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.*
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.basic.organizations.organizationContestsMenu
import com.saveourtool.save.frontend.components.basic.organizations.organizationSettingsMenu
import com.saveourtool.save.frontend.components.basic.organizations.organizationTestsMenu
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.smallTransparentModalStyle
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.http.getOrganization
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.HasSelectedMenu
import com.saveourtool.save.frontend.utils.changeUrl
import com.saveourtool.save.frontend.utils.urlAnalysis
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.getHighestRole
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes

import csstype.*
import history.Location
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
import react.router.dom.Link
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
    var location: Location
}

/**
 * [State] of project view component
 */
external interface OrganizationViewState : StateWithRole, State, HasSelectedMenu<OrganizationMenuBar> {
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
    var isErrorOpen: Boolean

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
    var isConfirmWindowOpen: Boolean

    /**
     * Label of confirm Window
     */
    var confirmLabel: String

    /**
     * Whether editing of organization info is disabled
     */
    var isEditDisabled: Boolean

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
    private val tableWithProjects: FC<TableProps<Project>> = tableComponent(
        columns = {
            columns<Project> {
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
            }
        },
        useServerPaging = false,
        usePageSelection = false,
    )
    private lateinit var responseFromDeleteOrganization: Response

    init {
        state.isUploading = false
        state.organization = Organization("", OrganizationStatus.CREATED, null, null, null)
        state.selectedMenu = OrganizationMenuBar.defaultTab
        state.projects = emptyArray()
        state.closeButtonLabel = null
        state.selfRole = Role.NONE
        state.draftOrganizationDescription = ""
        state.isConfirmWindowOpen = false
        state.isErrorOpen = false
        state.confirmationType = ConfirmationType.DELETE_CONFIRM
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

    override fun componentDidUpdate(prevProps: OrganizationProps, prevState: OrganizationViewState, snapshot: Any) {
        if (state.selectedMenu != prevState.selectedMenu) {
            changeUrl(state.selectedMenu, OrganizationMenuBar, "#/${props.organizationName}", "#/${OrganizationMenuBar.nameOfTheHeadUrlSection}/${props.organizationName}")
        } else if (props.location != prevProps.location) {
            urlAnalysis(OrganizationMenuBar, state.selfRole, state.organization?.canCreateContests)
        }
    }

    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            val organizationLoaded = getOrganization(props.organizationName)
            val projectsLoaded = getProjectsForOrganization()
            val role = getRoleInOrganization()
            val users = getUsers()
            val highestRole = getHighestRole(role, props.currentUserInfo?.globalRole)
            setState {
                organization = organizationLoaded
                image = ImageInfo(organizationLoaded.avatar)
                draftOrganizationDescription = organizationLoaded.description ?: ""
                projects = projectsLoaded
                isEditDisabled = true
                selfRole = highestRole
                usersInOrganization = users
            }
            urlAnalysis(OrganizationMenuBar, highestRole, organizationLoaded.canCreateContests)
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
    override fun ChildrenBuilder.render() {
        val errorCloseCallback = {
            setState {
                isErrorOpen = false
                closeButtonLabel = null
            }
        }
        displayModal(state.isErrorOpen, state.errorLabel, state.errorMessage, smallTransparentModalStyle, errorCloseCallback) {
            buttonBuilder(state.closeButtonLabel ?: "Close", "secondary") { errorCloseCallback() }
        }
        withNavigate {navigateContext->
            displayModal(state.isConfirmWindowOpen, state.confirmLabel, state.confirmMessage, smallTransparentModalStyle, { setState { isConfirmWindowOpen = false } }) {
                buttonBuilder("Ok") {
                    when (state.confirmationType) {
                        ConfirmationType.DELETE_CONFIRM -> navigateContext.deleteOrganizationBuilder()
                        else -> throw IllegalStateException("Not implemented yet")
                    }
                    setState { isConfirmWindowOpen = false }
                }
                buttonBuilder("Close", "secondary") {
                    setState { isConfirmWindowOpen = false }
                }
            }
        }

        renderOrganizationMenuBar()

        when (state.selectedMenu) {
            OrganizationMenuBar.INFO -> renderInfo()
            OrganizationMenuBar.TOOLS -> renderTools()
            OrganizationMenuBar.TESTS -> renderTests()
            OrganizationMenuBar.SETTINGS -> renderSettings()
            OrganizationMenuBar.CONTESTS -> renderContests()
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
            style = jso {
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
            div {
                className = ClassName("col-6")
                div {
                    className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Projects"
                }

                tableWithProjects {
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
                "$apiUrl/organizations/${props.organizationName}/update",
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

    private fun ChildrenBuilder.renderTests() {
        organizationTestsMenu {
            organizationName = props.organizationName
            selfRole = state.selfRole
        }
    }

    private fun ChildrenBuilder.renderContests() {
        organizationContestsMenu {
            organizationName = props.organizationName
            selfRole = state.selfRole
            updateErrorMessage = {
                setState {
                    isErrorOpen = true
                    errorLabel = ""
                    errorMessage = "Failed to create contest: ${it.status} ${it.statusText}"
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
            updateErrorMessage = { response, message ->
                setState {
                    isErrorOpen = true
                    errorLabel = response.statusText
                    errorMessage = message
                }
            }
            updateNotificationMessage = ::showNotification
            organization = state.organization ?: Organization.stub(-1)
            onCanCreateContestsChange = ::onCanCreateContestsChange
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

    private fun onCanCreateContestsChange(canCreateContests: Boolean) {
        val headers = jsonHeaders
        scope.launch {
            val response = post(
                "$apiUrl/organizations/${props.organizationName}/manage-contest-permission?isAbleToCreateContests=${!state.organization!!.canCreateContests}",
                headers,
                undefined,
                loadingHandler = ::classLoadingHandler,
            )
            if (response.ok) {
                setState {
                    organization = organization?.copy(canCreateContests = canCreateContests)
                }
            }
        }
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

    private fun ChildrenBuilder.renderTopProject(topProject: Project?) {
        div {
            className = ClassName("col-3 mb-4")
            topProject?.let {
                scoreCard {
                    name = it.name
                    contestScore = it.contestRating
                    url = "#/${props.organizationName}/${it.name}"
                }
            }
        }
    }

    @Suppress("LongMethod", "TOO_LONG_FUNCTION", "MAGIC_NUMBER")
    private fun ChildrenBuilder.renderOrganizationMenuBar() {
        div {
            className = ClassName("row d-flex justify-content-between")
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
                className = ClassName("col-auto mx-0")
                style = jso {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }

                nav {
                    className = ClassName("nav nav-tabs")
                    OrganizationMenuBar.values()
                        .filter {
                            it != OrganizationMenuBar.SETTINGS || state.selfRole.isHigherOrEqualThan(Role.ADMIN)
                        }
                        .filter {
                            it != OrganizationMenuBar.CONTESTS || state.selfRole.isHigherOrEqualThan(Role.OWNER) && state.organization?.canCreateContests == true
                        }
                        .forEach { organizationMenu ->
                            li {
                                className = ClassName("nav-item")
                                val classVal = if (state.selectedMenu == organizationMenu) " active font-weight-bold" else ""
                                p {
                                    className = ClassName("nav-link $classVal text-gray-800")
                                    onClick = {
                                        if (state.selectedMenu != organizationMenu) {
                                            setState { selectedMenu = organizationMenu }
                                        }
                                    }
                                    +organizationMenu.getTitle()
                                }
                            }
                        }
                }
            }

            div {
                className = ClassName("col-2 mr-auto")
                style = jso {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                }

                if (state.selfRole.isHigherOrEqualThan(Role.ADMIN)) {
                    Link {
                        to = "/${FrontendRoutes.CREATE_PROJECT.path}/${this@OrganizationView.state.organization?.name}"
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-outline-info")
                            +"Add Tool"
                        }
                    }
                }
            }
        }
    }

    private fun NavigateFunctionContext.deleteOrganizationBuilder() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            responseFromDeleteOrganization =
                    delete(
                        "$apiUrl/organizations/${props.organizationName}/delete",
                        headers,
                        body = undefined,
                        loadingHandler = ::noopLoadingHandler,
                    )
        }.invokeOnCompletion {
            if (responseFromDeleteOrganization.ok) {
                generateLinksWithSuffix<OrganizationMenuBar>(window.location.origin, "")
                // window.location.href = "${window.location.origin}/"
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
