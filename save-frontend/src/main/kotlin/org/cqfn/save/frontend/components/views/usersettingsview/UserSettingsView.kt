/**
 * A view with settings user
 */

package org.cqfn.save.frontend.components.views.usersettingsview

import org.cqfn.save.domain.ImageInfo
import org.cqfn.save.frontend.components.basic.InputTypes
import org.cqfn.save.frontend.components.views.AbstractView
import org.cqfn.save.frontend.externals.fontawesome.faEnvelope
import org.cqfn.save.frontend.externals.fontawesome.faKey
import org.cqfn.save.frontend.externals.fontawesome.faUser
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.http.getUser
import org.cqfn.save.frontend.utils.*
import org.cqfn.save.info.UserInfo
import org.cqfn.save.utils.AvatarType
import org.cqfn.save.v1

import csstype.*
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.fetch.Headers
import org.w3c.xhr.FormData
import react.*
import react.dom.*

import kotlinx.coroutines.launch
import kotlinx.html.InputType
import kotlinx.html.hidden
import kotlinx.html.js.onChangeFunction
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

    /**
     * Token for user
     */
    var token: String?
}

@Suppress("MISSING_KDOC_TOP_LEVEL")
abstract class UserSettingsView : AbstractView<UserSettingsProps, UserSettingsViewState>(false) {
    private val fieldsMap: MutableMap<InputTypes, String> = mutableMapOf()

    init {
        state.isUploading = false
    }

    /**
     * @param fieldName
     * @param target
     */
    fun changeFields(
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

    /**
     * @return element
     */
    abstract fun renderMenu(): FC<UserSettingsProps>

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    override fun RBuilder.render() {
        div("row justify-content-center") {
            // ===================== LEFT COLUMN =======================================================================
            div("col-2 mr-3") {
                div("card card-body mt-0 pt-0 pr-0 pl-0 border-secondary") {
                    div("col mr-2 pr-0 pl-0") {
                        attrs["style"] = kotlinx.js.jso<CSSProperties> {
                            background = "#e1e9ed".unsafeCast<Background>()
                        }
                        div("mb-0 font-weight-bold text-gray-800") {
                            form {
                                div("row g-3 ml-3 mr-3 pb-2 pt-2  border-bottom") {
                                    div("col-md-4 pl-0 pr-0") {
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
                                                    "/api/$v1/avatar$it"
                                                }
                                                    ?: run {
                                                        "img/user.svg"
                                                    }
                                                attrs.height = "60"
                                                attrs.width = "60"
                                            }
                                        }
                                    }
                                    div("col-md-6 pl-0") {
                                        attrs["style"] = kotlinx.js.jso<CSSProperties> {
                                            display = "flex".unsafeCast<Display>()
                                            alignItems = "center".unsafeCast<AlignItems>()
                                        }
                                        h1("h5 mb-0 text-gray-800") {
                                            +"${props.userName}"
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div("col mr-2 pr-0 pl-0") {
                        nav {
                            div("pl-3 ui vertical menu profile-setting") {
                                form {
                                    div("item mt-2") {
                                        div("header") {
                                            +"Basic Setting"
                                        }
                                        div("menu") {
                                            div("mt-2") {
                                                a(classes = "item", href = "#/${props.userName}/settings/profile") {
                                                    fontAwesomeIcon {
                                                        attrs.icon = faUser
                                                        attrs.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                                                    }
                                                    +"Profile"
                                                }
                                            }
                                            div("mt-2") {
                                                a(classes = "item", href = "#/${props.userName}/settings/email") {
                                                    fontAwesomeIcon {
                                                        attrs.icon = faEnvelope
                                                        attrs.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                                                    }
                                                    +"Email management"
                                                }
                                            }
                                        }
                                    }
                                }
                                form {
                                    div("item mt-2") {
                                        div("header") {
                                            +"Security Setting"
                                        }
                                        div("menu") {
                                            div("mt-2") {
                                                a(classes = "item", href = "#/${props.userName}/settings/token") {
                                                    fontAwesomeIcon {
                                                        attrs.icon = faKey
                                                        attrs.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                                                    }
                                                    +"Personal access tokens"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===================== RIGHT COLUMN =======================================================================
            div("col-6") {
                child(renderMenu())
            }
        }
    }

    @Suppress("MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
    fun updateUser() {
        println("\nSTATE ${state.userInfo?.name}")
        val newUserInfo = UserInfo(
            name = state.userInfo!!.name,
            password = state.token,
            source = state.userInfo!!.source,
            projects = state.userInfo!!.projects,
            email = fieldsMap[InputTypes.USER_EMAIL]?.trim(),
            company = fieldsMap[InputTypes.COMPANY]?.trim(),
            location = fieldsMap[InputTypes.LOCATION]?.trim(),
            linkedin = fieldsMap[InputTypes.LINKEDIN]?.trim(),
            gitHub = fieldsMap[InputTypes.GIT_HUB]?.trim(),
            twitter = fieldsMap[InputTypes.TWITTER]?.trim(),
            avatar = state.userInfo!!.avatar,
        )

        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            println("SENT REQUEST TO UPDATE USER INFO")
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
                        "$apiUrl/image/upload?owner=${props.userName}&type=${AvatarType.USER}",
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
