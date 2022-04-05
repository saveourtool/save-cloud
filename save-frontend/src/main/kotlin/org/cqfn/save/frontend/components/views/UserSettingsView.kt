/**
 * A view with settings user
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.frontend.utils.*
import org.cqfn.save.info.UserInfo

import csstype.Position
import csstype.TextAlign
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.xhr.FormData
import react.*
import react.dom.*

import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.hidden
import kotlinx.html.js.onChangeFunction

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface UserSettingsProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?
}

/**
 * [State] of project view component
 */
@Suppress("MISSING_KDOC_TOP_LEVEL")
external interface UserSettingsViewState : State {
    /**
     * Flag to handle uploading a file
     */
    var isUploading: Boolean

    /**
     * Image to owner avatar
     */
    var image: ImageInfo?
}

@Suppress("MISSING_KDOC_TOP_LEVEL")
class UserSettingsView : AbstractView<UserSettingsProps, UserSettingsViewState>(false) {
    init {
        state.isUploading = false
    }

    override fun componentDidMount() {
        super.componentDidMount()
        scope.launch {
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
                +"${props.userInfo?.userName}"
            }
        }

        div("row justify-content-center") {
            // ===================== LEFT COLUMN =======================================================================
            div("col-2 mr-3") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Profile picture"
                }

                div {
                    attrs["style"] = kotlinx.js.jso<CSSProperties> {
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
                            attrs.src = state.image?.path?.let {
                                "/api/avatar$it"
                            }
                                ?: run {
                                    "img/user.svg"
                                }
                            attrs.height = "260"
                            attrs.width = "260"
                        }
                    }
                }
            }

            // ===================== RIGHT COLUMN =======================================================================
            div("col-6") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Profile info"
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
                        "$apiUrl/image/upload?owner=${props.userInfo?.userName}&isOrganization=false",
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

    private suspend fun getAvatar() = get("$apiUrl/users/${props.userInfo?.userName}/avatar", Headers(),
        responseHandler = ::noopResponseHandler)
        .unsafeMap {
            it.decodeFromJsonString<ImageInfo>()
        }
}
