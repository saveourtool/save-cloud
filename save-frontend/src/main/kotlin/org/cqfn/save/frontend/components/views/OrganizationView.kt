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
    var selectedMenu: ProjectMenuBar?
}

/**
 * A Component for owner view
 */
class OrganizationView : AbstractView<OrganizationProps, OrganizationViewState>(false) {
    init {
        state.isUploading = false
        state.organization = Organization("", null, null, null)
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

        // ================= row for avatar and name ==================
        div("row") {
            div("col-3 mx-auto") {
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
                        attrs.height = "50"
                        attrs.width = "50"
                    }
                }


                attrs["style"] = jso<CSSProperties> {
                    display = Display.flex
                    alignItems = AlignItems.center
                }
                h1("h3 mb-0 text-gray-800 ml-2") {
                    +"${state.organization?.name}"
                }
            }


            div("col-3 mx-auto") {
                attrs["style"] = jso<CSSProperties> {
                    justifyContent = JustifyContent.flexEnd
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

        // ============================ row for a tabs menu ===============
            div("row align-items-center justify-content-center") {
                nav("nav nav-tabs mb-4") {
                    ProjectMenuBar.values().forEachIndexed { i, projectMenu ->
                        li("nav-item") {
                            val classVal = if ((i == 0 && state.selectedMenu == null) || state.selectedMenu == projectMenu) " active font-weight-bold" else ""
                            p("nav-link $classVal text-gray-800") {
                                attrs.onClickFunction = {
                                    if (state.selectedMenu != projectMenu) {
                                        setState {
                                            selectedMenu = projectMenu
                                        }
                                    }
                                    if (projectMenu != ProjectMenuBar.STATISTICS) {
                                        // openMenuStatisticFlag(false)
                                    }
                                    if (projectMenu != ProjectMenuBar.SETTINGS) {
                                        // openMenuSettingsFlag(false)
                                    }
                                }
                                +projectMenu.name
                            }
                        }
                    }
                }
        }

        // ============================ row for a tabs menu ===============


        div("row") {
            div("col-3 mx-auto") {
                attrs["style"] = jso<CSSProperties> {
                    display = Display.flex
                    alignItems = AlignItems.center
                }

                div("position-relative") {
                    attrs["style"] = jso<CSSProperties> {
                        position = "relative".unsafeCast<Position>()
                        textAlign = "center".unsafeCast<TextAlign>()
                    }
                    img(classes = "width-full color-bg-default") {
                        attrs.src = "img/green_square.png"
                        attrs.height = "200"
                        attrs.width = "200"
                    }
                    div("position-absolute") {
                        attrs["style"] = jso<CSSProperties> {
                            top = "40%".unsafeCast<Top>()
                            left = "40%".unsafeCast<Left>()
                        }
                        // fixme: It must be replaced with the current value after creating the calculated rating.
                        h1(" mb-0 text-gray-800") {
                            +"4.5"
                        }
                    }
                }
            }
            div("col-3 mx-auto") {

            }
            div("col-3 mx-auto") {

            }
        }

        div("d-sm-flex align-items-center justify-content-center mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +"${state.organization?.name}"
            }
        }

        div("row justify-content-center") {
            // ===================== LEFT COLUMN =======================================================================
            div("col-2 mr-3") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Organization"
                }



                div("position-relative") {
                    attrs["style"] = jso<CSSProperties> {
                        position = "relative".unsafeCast<Position>()
                        textAlign = "center".unsafeCast<TextAlign>()
                    }
                    img(classes = "width-full color-bg-default") {
                        attrs.src = "img/green_square.png"
                        attrs.height = "200"
                        attrs.width = "200"
                    }
                    div("position-absolute") {
                        attrs["style"] = jso<CSSProperties> {
                            top = "40%".unsafeCast<Top>()
                            left = "40%".unsafeCast<Left>()
                        }
                        // fixme: It must be replaced with the current value after creating the calculated rating.
                        h1(" mb-0 text-gray-800") {
                            +"4.5"
                        }
                    }
                }
            }

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
