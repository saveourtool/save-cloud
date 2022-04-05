/**
 * A view with settings user
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.frontend.components.basic.InputTypes
import org.cqfn.save.frontend.components.basic.cardComponent
import org.cqfn.save.frontend.http.getUser
import org.cqfn.save.frontend.utils.*
import org.cqfn.save.info.UserInfo

import csstype.Position
import csstype.TextAlign
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.fetch.Headers
import org.w3c.xhr.FormData
import react.*
import react.dom.*

import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.hidden
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface UserSettingsProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userName: String?
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

    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?
}

@Suppress("MISSING_KDOC_TOP_LEVEL")
class UserSettingsView : AbstractView<UserSettingsProps, UserSettingsViewState>(false) {
    private val fieldsMap: MutableMap<InputTypes, String> = mutableMapOf()

    init {
        state.isUploading = false
    }

    private fun changeFields(
        fieldName: InputTypes,
        target: Event,
    ) {
        val tg = target.target as HTMLInputElement
        val value = tg.value
        fieldsMap[fieldName] = value
    }

    override fun componentDidMount() {
        super.componentDidMount()
        scope.launch {
            val avatar = getAvatar()
            val user = props.userName?.let { getUser(it) }
            setState {
                image = avatar
                userInfo = user
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    override fun RBuilder.render() {
        div("d-sm-flex align-items-center justify-content-center mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +"${props.userName}"
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

                child(cardComponent(isBordered = false, hasBg = true) {
                    div("row mt-2 ml-2 mr-2") {
                        div("col-5 text-left align-self-center") {
                            +"User email:"
                        }
                        div("col-7 input-group pl-0") {
                            input(type = InputType.email) {
                                attrs["class"] = "form-control"
                                attrs {
                                    state.userInfo?.email?.let {
                                        defaultValue = it
                                    }
                                    placeholder = "email@example.com"
                                    onChangeFunction = {
                                        changeFields(InputTypes.USER_EMAIL, it)
                                    }
                                }
                            }
                        }
                    }

                    hr("") {}
                    div("row d-flex justify-content-center") {
                        div("col-3 d-sm-flex align-items-center justify-content-center") {
                            button(type = ButtonType.button, classes = "btn btn-sm btn-primary") {
                                attrs.onClickFunction = {
                                    updateUser()
                                }
                                +"Save changes"
                            }
                        }
                    }
                })
            }
        }
    }

    private fun updateUser() {
        val newUserInfo = UserInfo(
            state.userInfo!!.userName,
            fieldsMap[InputTypes.USER_EMAIL]?.trim(),
            state.userInfo?.avatar,
        )

        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            post("$apiUrl/users/save", headers, Json.encodeToString(newUserInfo))
        }
    }

    private fun postImageUpload(element: HTMLInputElement) =
            scope.launch {
                setState {
                    isUploading = true
                }
                element.files!!.asList().single().let { file ->
                    val response: ImageInfo? = post(
                        "$apiUrl/image/upload?owner=${props.userName}&isOrganization=false",
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

    private suspend fun getAvatar() = get("$apiUrl/users/${props.userName}/avatar", Headers(),
        responseHandler = ::noopResponseHandler)
        .unsafeMap {
            it.decodeFromJsonString<ImageInfo>()
        }
}
