/**
 * A view with owner details
 */

package org.cqfn.save.frontend.components.views

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.basic.privacySpan
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.utils.*
import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.table.columns
import csstype.Position
import csstype.Top
import csstype.Left
import csstype.TextAlign
import kotlinx.html.InputType
import kotlinx.html.hidden
import kotlinx.html.js.onChangeFunction
import org.cqfn.save.domain.ImageInfo
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.xhr.FormData

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface OwnerProps : PropsWithChildren {
    var owner: String
}

/**
 * [State] of project view component
 */
external interface OwnerViewState : State {

    /**
     * Flag to handle uploading a file
     */
    var isUploading: Boolean

    /**
     * Image to owner avatar
     */
    var image: ImageInfo?
}

/**
 * A Component for owner view
 */
class OrganizationView : AbstractView<OwnerProps, OwnerViewState>(false) {

    init {
        state.isUploading = false
    }

    override fun componentDidMount() {
        super.componentDidMount()
        GlobalScope.launch {
            val avatar = getAvatar()
            setState {
                image = avatar
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    override fun RBuilder.render() {

        div("d-sm-flex align-items-center justify-content-center mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +props.owner
            }
        }

        div("row justify-content-center") {
            // ===================== LEFT COLUMN =======================================================================
            div("col-2 mr-3") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Owner"
                }

                div {
                    attrs["style"] = kotlinext.js.jso<CSSProperties> {
                        position = "relative".unsafeCast<Position>()
                        textAlign = "center".unsafeCast<TextAlign>()
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
                        attrs["aria-label"] = "Change avatar owner"
                        img(classes = "avatar avatar-user width-full border color-bg-default rounded-circle") {
                            attrs.src = if (state.image?.base64 != null) ("data:image/png;base64, " + state.image?.base64) else "img/image_not_found.png"
                            attrs.height = "260"
                            attrs.width = "260"
                        }
                    }
                }

                div("position-relative") {
                    attrs["style"] = kotlinext.js.jso<CSSProperties> {
                        position = "relative".unsafeCast<Position>()
                        textAlign = "center".unsafeCast<TextAlign>()
                    }
                    img(classes = "width-full color-bg-default") {
                        attrs.src = "img/green_square.png"
                        attrs.height = "200"
                        attrs.width = "200"
                    }
                    div("position-absolute") {
                        attrs["style"] = kotlinext.js.jso<CSSProperties> {
                            top = "40%".unsafeCast<Top>()
                            left = "40%".unsafeCast<Left>()
                        }
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
                        column(id = "passed", header = "Description") {
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
                        url = "$apiUrl/projects/not-deleted",
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
        GlobalScope.launch {
            setState {
                isUploading = true
            }
            element.files!!.asList().single().let { file ->
                val response: ImageInfo = post(
                    "$apiUrl/image/upload?owner=${props.owner}",
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

    private suspend fun getAvatar() = get("$apiUrl/avatar?owner=${props.owner}", Headers())
        .unsafeMap {
            it.decodeFromJsonString<ImageInfo>()
        }

}
