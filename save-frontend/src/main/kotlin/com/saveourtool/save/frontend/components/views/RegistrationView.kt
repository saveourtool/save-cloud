/**
 * A view for registration
 */

@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.ImageInfo
import com.saveourtool.save.frontend.components.basic.InputTypes
import com.saveourtool.save.frontend.http.getUser
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.xhr.FormData
import react.ChildrenBuilder
import react.PropsWithChildren
import react.State
import react.dom.aria.ariaLabel
import react.dom.events.ChangeEvent
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * `Props` retrieved from router
 */
external interface RegistrationProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userName: String?
}

/**
 * [State] of registration view component
 */
external interface RegistrationViewState : State {
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
     * Validation of input fields
     */
    var isValidUserName: Boolean?
}

/**
 * A Component for registration view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class RegistrationView : AbstractView<RegistrationProps, RegistrationViewState>() {
    private val fieldsMap: MutableMap<InputTypes, String> = mutableMapOf()

    init {
        state.isValidUserName = true
        state.isUploading = false
    }

    override fun componentDidMount() {
        super.componentDidMount()
        scope.launch {
            val user = props.userName
                ?.let { getUser(it) }
            setState {
                userInfo = user
                userInfo?.let { updateFieldsMap(it) }
            }
        }
    }

    private fun changeFields(
        fieldName: InputTypes,
        target: ChangeEvent<HTMLInputElement>,
    ) {
        val tg = target.target
        fieldsMap[fieldName] = tg.value
    }

    private fun saveUser() {
        val newUserInfo = state.userInfo?.copy(
            name = fieldsMap[InputTypes.USER_NAME]?.trim() ?: state.userInfo!!.name,
            oldNames = listOf(state.userInfo!!.name),
            isActive = true,
        )

        scope.launch {
            val response = post(
                "$apiUrl/users/save",
                jsonHeaders,
                Json.encodeToString(newUserInfo),
                loadingHandler = ::classLoadingHandler,
            )
            if (response.ok) {
                window.location.href = "#/${FrontendRoutes.PROJECTS.path}"
            }
        }
    }

    private fun updateFieldsMap(userInfo: UserInfo) {
        userInfo.name.let { fieldsMap[InputTypes.USER_NAME] = it }
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "LongMethod",
        "MAGIC_NUMBER"
    )
    override fun ChildrenBuilder.render() {
        if (state.userInfo?.isActive == false) {
            ReactHTML.main {
                className = ClassName("main-content mt-0 ps")
                ReactHTML.div {
                    className = ClassName("page-header align-items-start min-vh-100")
                    ReactHTML.span {
                        className = ClassName("mask bg-gradient-dark opacity-6")
                    }
                    ReactHTML.div {
                        className = ClassName("row justify-content-center")
                        ReactHTML.div {
                            className = ClassName("col-sm-4")
                            ReactHTML.div {
                                className = ClassName("container card o-hidden border-0 shadow-lg my-2 card-body p-0")
                                ReactHTML.div {
                                    className = ClassName("p-5 text-center")
                                    ReactHTML.h1 {
                                        className = ClassName("h4 text-gray-900 mb-4")
                                        +"Registration new user"
                                    }
                                    ReactHTML.label {
                                        ReactHTML.input {
                                            type = InputType.file
                                            hidden = true
                                            onChange = { event ->
                                                postImageUpload(event.target)
                                            }
                                        }
                                        ariaLabel = "Change organization's avatar"
                                        ReactHTML.img {
                                            className =
                                                    ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                            src = state.image?.path?.let {
                                                "/api/$v1/avatar$it"
                                            }
                                                ?: run {
                                                    "img/user.svg"
                                                }
                                            height = 260.0
                                            width = 260.0
                                        }
                                    }
                                    ReactHTML.form {
                                        className = ClassName("needs-validation")
                                        ReactHTML.label {
                                            className = ClassName("form-label")
                                            +"User name"
                                        }
                                        ReactHTML.div {
                                            ReactHTML.span {
                                                className = ClassName("input-group-text")
                                                +"*"
                                            }
                                            className = ClassName("mt-2 input-group pl-0")
                                            ReactHTML.input {
                                                type = InputType.text
                                                className = ClassName("form-control")
                                                state.userInfo?.name?.let {
                                                    defaultValue = it
                                                }
                                                onChange = {
                                                    changeFields(InputTypes.USER_NAME, it)
                                                }
                                            }
                                        }
                                        ReactHTML.button {
                                            type = ButtonType.button
                                            className = ClassName("btn btn-info mt-4 mr-3")
                                            +"Registration"
                                            onClick = {
                                                saveUser()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (state.userInfo?.isActive == true) {
            window.location.href = "#/${FrontendRoutes.PROJECTS.path}"
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
                        },
                        loadingHandler = ::classLoadingHandler,
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
}
