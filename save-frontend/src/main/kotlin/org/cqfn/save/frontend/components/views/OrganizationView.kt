/**
 * A view with organization details
 */

package org.cqfn.save.frontend.components.views

import csstype.*
import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.basic.privacySpan
import org.cqfn.save.frontend.components.errorStatusContext
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.http.getOrganization
import org.cqfn.save.frontend.utils.*

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
import org.cqfn.save.frontend.externals.fontawesome.*

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
}

/**
 * A Component for owner view
 */
class OrganizationView : AbstractView<OrganizationProps, OrganizationViewState>(false) {
    init {
        state.isUploading = false
        state.organization = Organization("", null, null, null)
        state.selectedMenu = OrganizationMenuBar.INFO
    }

    override fun componentDidMount() {
        super.componentDidMount()
        scope.launch {
            val avatar = getAvatar()
            val organizationNew = getOrganization(props.organizationName)
            setState {
                image = avatar
                organization = organizationNew
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
    override fun RBuilder.render() {

        if (state.selectedMenu == OrganizationMenuBar.INFO) {
            // ================= row for avatar and name ==================
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


            // ============================ row for TOP projects ===============
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

            // ============================ row for TOP projects ===============
            div("row") {
                attrs["style"] = jso<CSSProperties> {
                    justifyContent = JustifyContent.center
                }

                div("col-3 mb-4") {
                    div("card border-left-info shadow h-70 py-2") {
                        div("card-body") {
                            div("row no-gutters align-items-center") {
                                div("col mr-2") {
                                    div("row") {
                                        div("text-xs font-weight-bold text-info text-uppercase mb-1 ml-2") {
                                            attrs["style"] = jso<CSSProperties> {
                                                justifyContent = JustifyContent.center
                                                display = Display.flex
                                                alignItems = AlignItems.center
                                            }

                                            +"Rating"
                                        }
                                        div("col") {
                                            attrs["style"] = jso<CSSProperties> {
                                                justifyContent = JustifyContent.center
                                                display = Display.flex
                                                alignItems = AlignItems.center
                                            }

                                            h6 {
                                                +"Diktat 0.1.0"
                                            }
                                        }
                                    }
                                    div("row no-gutters align-items-center") {
                                        div("col-auto") {
                                            div("h5 mb-0 mr-3 font-weight-bold text-gray-800") { +"10" }
                                        }
                                        div("col") {
                                            div("progress progress-sm mr-2") {
                                                div("progress-bar bg-info") {
                                                    attrs["role"] = "progressbar"
                                                    attrs["style"] = jso<CSSProperties> {
                                                        width = "10%".unsafeCast<Width>()
                                                    }
                                                    attrs["aria-valuenow"] = "100"
                                                    attrs["aria-valuemin"] = "0"
                                                    attrs["aria-valuemax"] = "100"
                                                }
                                            }
                                        }
                                    }
                                }
                                div("col-auto") {
                                    i("fas fa-clipboard-list fa-2x text-gray-300") {
                                    }
                                }
                            }
                        }
                    }
                }

                div("col-3 mb-4") {
                    div("card border-left-info shadow h-70 py-2") {
                        div("card-body") {
                            div("row no-gutters align-items-center") {
                                div("col mr-2") {
                                    div("row") {
                                        div("text-xs font-weight-bold text-info text-uppercase mb-1 ml-2") {
                                            attrs["style"] = jso<CSSProperties> {
                                                justifyContent = JustifyContent.center
                                                display = Display.flex
                                                alignItems = AlignItems.center
                                            }

                                            +"Rating"
                                        }
                                        div("col") {
                                            attrs["style"] = jso<CSSProperties> {
                                                justifyContent = JustifyContent.center
                                                display = Display.flex
                                                alignItems = AlignItems.center
                                            }

                                            h6 {
                                                +"Diktat 1.0.4"
                                            }
                                        }
                                    }
                                    div("row no-gutters align-items-center") {
                                        div("col-auto") {
                                            div("h5 mb-0 mr-3 font-weight-bold text-gray-800") { +"80" }
                                        }
                                        div("col") {
                                            div("progress progress-sm mr-2") {
                                                div("progress-bar bg-info") {
                                                    attrs["role"] = "progressbar"
                                                    attrs["style"] = jso<CSSProperties> {
                                                        width = "80%".unsafeCast<Width>()
                                                    }
                                                    attrs["aria-valuenow"] = "100"
                                                    attrs["aria-valuemin"] = "0"
                                                    attrs["aria-valuemax"] = "100"
                                                }
                                            }
                                        }
                                    }
                                }
                                div("col-auto") {
                                    i("fas fa-clipboard-list fa-2x text-gray-300") {
                                    }
                                }
                            }
                        }
                    }
                }
            }

            div("row") {
                attrs["style"] = jso<CSSProperties> {
                    justifyContent = JustifyContent.center
                }

                div("col-3 mb-4") {
                    div("card border-left-info shadow h-70 py-2") {
                        div("card-body") {
                            div("row no-gutters align-items-center") {
                                div("col mr-2") {
                                    div("row") {
                                        div("text-xs font-weight-bold text-info text-uppercase mb-1 ml-2") {
                                            attrs["style"] = jso<CSSProperties> {
                                                justifyContent = JustifyContent.center
                                                display = Display.flex
                                                alignItems = AlignItems.center
                                            }

                                            +"Rating"
                                        }
                                        div("col") {
                                            attrs["style"] = jso<CSSProperties> {
                                                justifyContent = JustifyContent.center
                                                display = Display.flex
                                                alignItems = AlignItems.center
                                            }

                                            h6 {
                                                +"Diktat 1.0.3"
                                            }
                                        }
                                    }
                                    div("row no-gutters align-items-center") {
                                        div("col-auto") {
                                            div("h5 mb-0 mr-3 font-weight-bold text-gray-800") { +"60" }
                                        }
                                        div("col") {
                                            div("progress progress-sm mr-2") {
                                                div("progress-bar bg-info") {
                                                    attrs["role"] = "progressbar"
                                                    attrs["style"] = jso<CSSProperties> {
                                                        width = "60%".unsafeCast<Width>()
                                                    }
                                                    attrs["aria-valuenow"] = "100"
                                                    attrs["aria-valuemin"] = "0"
                                                    attrs["aria-valuemax"] = "100"
                                                }
                                            }
                                        }
                                    }
                                }
                                div("col-auto") {
                                    i("fas fa-clipboard-list fa-2x text-gray-300") {
                                    }
                                }
                            }
                        }
                    }
                }


                div("col-3 mb-4") {
                    div("card border-left-info shadow h-70 py-2") {
                        div("card-body") {
                            div("row no-gutters align-items-center") {
                                div("col mr-2") {
                                    div("row") {
                                        div("text-xs font-weight-bold text-info text-uppercase mb-1 ml-2") {
                                            attrs["style"] = jso<CSSProperties> {
                                                justifyContent = JustifyContent.center
                                                display = Display.flex
                                                alignItems = AlignItems.center
                                            }

                                            +"Rating"
                                        }
                                        div("col") {
                                            attrs["style"] = jso<CSSProperties> {
                                                justifyContent = JustifyContent.center
                                                display = Display.flex
                                                alignItems = AlignItems.center
                                            }

                                            h6 {
                                                +"Diktat 1.0.0"
                                            }
                                        }
                                    }
                                    div("row no-gutters align-items-center") {
                                        div("col-auto") {
                                            div("h5 mb-0 mr-3 font-weight-bold text-gray-800") { +"50" }
                                        }
                                        div("col") {
                                            div("progress progress-sm mr-2") {
                                                div("progress-bar bg-info") {
                                                    attrs["role"] = "progressbar"
                                                    attrs["style"] = jso<CSSProperties> {
                                                        width = "50%".unsafeCast<Width>()
                                                    }
                                                    attrs["aria-valuenow"] = "100"
                                                    attrs["aria-valuemin"] = "0"
                                                    attrs["aria-valuemax"] = "100"
                                                }
                                            }
                                        }
                                    }
                                }
                                div("col-auto") {
                                    i("fas fa-clipboard-list fa-2x text-gray-300") {
                                    }
                                }
                            }
                        }
                    }
                }
            }

            div("row") {
                attrs["style"] = jso<CSSProperties> {
                    justifyContent = JustifyContent.center
                }
                div("col-3 mb-4") {
                    div("card shadow mb-4") {
                        div("card-header py-3") {
                            h6("m-0 font-weight-bold text-primary") { +"Description" }
                        }
                        div("card-body") {
                            p {
                                +"""As a group of enthusiasts who create """

                                a("https://github.com/analysis-dev/") {
                                    +"""dev-tools"""
                                }

                                +""" (including static analysis tools),
                                            we have seen a lack of materials related to testing scenarios or benchmarks that can be used to evaluate and test our applications.
                                            
                                            So we decided to create this """

                                a("https://github.com/analysis-dev/awesome-benchmarks") {
                                    +"""curated list of standards, tests and benchmarks"""
                                }

                                +""" that can be used for testing and evaluating dev tools.
                                            Our focus is mainly on the code analysis, but is not limited by this category,
                                             in this list we are trying to collect all benchmarks that could be useful 
                                             for creators of dev-tools."""
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
                            div("col-md-4") {
                                figure("mb-0") {
                                    img(classes = "img-fluid") {
                                        attrs["src"] = "https://bootdey.com/img/Content/avatar/avatar9.png"
                                        attrs["alt"] = ""
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.selectedMenu == OrganizationMenuBar.TOOLS) {

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
                            column(id = "name", header = "Evaluated Tool", { name }) {
                                buildElement {
                                    td {
                                        a(href = "#/${it.row.original.organization.name}/${it.value}") { +it.value }
                                        privacySpan(it.row.original)
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
                        initialPageSize = 10,
                        useServerPaging = false,
                        usePageSelection = false,
                    ) { _, _ ->
                        get(
                            url = "$apiUrl/projects/get/projects-by-organization?organizationName=${props.organizationName}",
                            headers = Headers().also {
                                it.set("Accept", "application/json")
                            },
                        )
                            .unsafeMap {
                                it.decodeFromJsonString<Array<Project>>()
                            }
                    }) { }
                }
            }

        }
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
    )
        .unsafeMap {
            it.decodeFromJsonString<ImageInfo>()
        }

    companion object :
        RStatics<OrganizationProps, OrganizationViewState, OrganizationView, Context<StateSetter<Response?>>>(
            OrganizationView::class
        ) {
        init {
            contextType = errorStatusContext
        }
    }
}

