/**
 * A view with organization details
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.basic.privacySpan
import org.cqfn.save.frontend.components.errorStatusContext
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.externals.fontawesome.*
import org.cqfn.save.frontend.http.getOrganization
import org.cqfn.save.frontend.utils.*

import csstype.*
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.xhr.FormData
import react.*
import react.dom.*
import react.table.columns

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
     * Whether editing of organization info is disabled
     */
    var isEditDisabled: Boolean?

    /**
     * Role user in organization
     */
    var roleInOrganization: Role?
}

/**
 * A Component for owner view
 */
class OrganizationView : AbstractView<OrganizationProps, OrganizationViewState>(false) {
    private var descriptionTmp: String = ""

    init {
        state.isUploading = false
        state.organization = Organization("", null, null, null)
        state.selectedMenu = OrganizationMenuBar.INFO
        state.projects = emptyArray()
    }

    override fun componentDidMount() {
        super.componentDidMount()
        scope.launch {
            val avatar = getAvatar()
            val organizationLoaded = getOrganization(props.organizationName)
            val projectsLoaded = getProjectsForOrganization()
            val role = getRoleInOrganization()
            setState {
                image = avatar
                organization = organizationLoaded
                projects = projectsLoaded
                roleInOrganization = role
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
    override fun RBuilder.render() {
        when (state.selectedMenu!!) {
            OrganizationMenuBar.INFO -> renderInfo()
            OrganizationMenuBar.TOOLS -> renderTools()
            OrganizationMenuBar.SETTINGS -> renderSettings()
            else -> {
                // this is a generated else block
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
    private fun RBuilder.renderInfo() {
        // ================= Row for avatar and name =============
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
                            "/api/avatar$it"
                        }
                            ?: run {
                                "img/company.svg"
                            }
                        attrs.height = "100"
                        attrs.width = "100"
                    }
                }

                h1("h3 mb-0 text-gray-800 ml-2") {
                    +"${state.organization?.name}"
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
                            if ((state.roleInOrganization?.priority ?: 0) >= 2) {
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
                        input(type = InputType.text) {
                            attrs["class"] = "form-control-plaintext pt-0 pb-0"
                            if (state.isEditDisabled != false) {
                                attrs.value = state.organization?.description ?: ""
                            }
                            attrs.disabled = state.isEditDisabled ?: true
                            attrs.onChange = { event ->
                                val tg = event.target as HTMLInputElement
                                setNewDescription(tg.value)
                            }
                        }
                    }
                    div("ml-3 mt-2 align-items-right float-right") {
                        button(type = ButtonType.button, classes = "btn") {
                            fontAwesomeIcon(icon = faCheck)
                            attrs.hidden = state.isEditDisabled ?: true
                            attrs.onClick = {
                                onOrganizationSave()
                                turnEditMode(true)
                            }
                        }

                        button(type = ButtonType.button, classes = "btn") {
                            fontAwesomeIcon(icon = faTimesCircle)
                            attrs.hidden = state.isEditDisabled ?: true
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
                        div("col-md-4") {
                            figure {
                                img(classes = "img-fluid") {
                                    attrs["src"] = "https://bootdey.com/img/Content/avatar/avatar1.png"
                                    attrs["alt"] = ""
                                }
                            }
                        }
                        div("col-md-4") {
                            figure {
                                img(classes = "img-fluid") {
                                    attrs["src"] = "https://bootdey.com/img/Content/avatar/avatar2.png"
                                    attrs["alt"] = ""
                                }
                            }
                        }
                        div("col-md-4") {
                            figure {
                                img(classes = "img-fluid") {
                                    attrs["src"] = "https://bootdey.com/img/Content/avatar/avatar3.png"
                                    attrs["alt"] = ""
                                }
                            }
                        }
                        div("col-md-4") {
                            figure {
                                img(classes = "img-fluid") {
                                    attrs["src"] = "https://bootdey.com/img/Content/avatar/avatar4.png"
                                    attrs["alt"] = ""
                                }
                            }
                        }
                        div("col-md-4") {
                            figure {
                                img(classes = "img-fluid") {
                                    attrs["src"] = "https://bootdey.com/img/Content/avatar/avatar5.png"
                                    attrs["alt"] = ""
                                }
                            }
                        }
                        div("col-md-4") {
                            figure {
                                img(classes = "img-fluid") {
                                    attrs["src"] = "https://bootdey.com/img/Content/avatar/avatar6.png"
                                    attrs["alt"] = ""
                                }
                            }
                        }
                        div("col-md-4") {
                            figure("mb-0") {
                                img(classes = "img-fluid") {
                                    attrs["src"] = "https://bootdey.com/img/Content/avatar/avatar7.png"
                                    attrs["alt"] = ""
                                }
                            }
                        }
                        div("col-md-4") {
                            figure("mb-0") {
                                img(classes = "img-fluid") {
                                    attrs["src"] = "https://bootdey.com/img/Content/avatar/avatar8.png"
                                    attrs["alt"] = ""
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    private fun RBuilder.renderTools() {
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
                            "/api/avatar$it"
                        }
                            ?: run {
                                "img/company.svg"
                            }
                        attrs.height = "100"
                        attrs.width = "100"
                    }
                }

                h1("h3 mb-0 text-gray-800 ml-2") {
                    +"${state.organization?.name}"
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

        div("row justify-content-center") {
            // ===================== RIGHT COLUMN =======================================================================
            div("col-6") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Projects"
                }

                child(tableComponent(
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
                ) { _, _ ->
                    getProjectsFromCache()
                }) { }
            }
        }
    }

    private fun setNewDescription(value: String) {
        descriptionTmp = value
    }

    private fun onOrganizationSave() {
        setState {
            organization?.apply {
                description = descriptionTmp
            }
        }
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            post("$apiUrl/organization/update", headers, Json.encodeToString(state.organization))
        }
    }

    private fun RBuilder.renderSettings() {
        // FixMe: will be finished later
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
        url = "$apiUrl/projects/get/projects-by-organization?organizationName=${props.organizationName}",
        headers = Headers().also {
            it.set("Accept", "application/json")
        },
    )
        .unsafeMap {
            it.decodeFromJsonString()
        }

    private suspend fun getRoleInOrganization(): Role = get(
        url = "$apiUrl/organization/role?organization=${props.organizationName}",
        headers = Headers().also {
            it.set("Accept", "application/json")
        },
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
                        "$apiUrl/image/upload?owner=${props.organizationName}",
                        Headers(),
                        FormData().apply {
                            append("file", file)
                        }
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
        "$apiUrl/organization/${props.organizationName}/avatar", Headers(),
        responseHandler = ::noopResponseHandler
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

    companion object :
        RStatics<OrganizationProps, OrganizationViewState, OrganizationView, Context<StateSetter<Response?>>>(
        OrganizationView::class
    ) {
        const val TOP_PROJECTS_NUMBER = 4
        init {
            contextType = errorStatusContext
        }
    }
}
