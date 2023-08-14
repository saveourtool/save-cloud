/**
 * A view for registration
 */

@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.inputform.inputTextFormRequired
import com.saveourtool.save.frontend.components.modal.MAX_Z_INDEX
import com.saveourtool.save.frontend.http.postImageUpload
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.info.UserStatus
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes
import com.saveourtool.save.validation.isValidLengthName
import com.saveourtool.save.validation.isValidName

import js.core.asList
import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span
import react.router.dom.Link
import react.router.useNavigate
import web.cssom.ClassName
import web.cssom.ZIndex
import web.cssom.rem
import web.file.File
import web.html.InputType
import web.window.WindowTarget

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A Component for registration view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
val registrationView: FC<RegistrationProps> = FC { props ->
    useBackground(Style.INDEX)
    particles()
    val (isTermsOfUseOk, setIsTermsOfUseOk) = useState(false)
    val (conflictErrorMessage, setConflictErrorMessage) = useState<String?>(null)
    val (userInfo, setUserInfo) = useStateFromProps(props.userInfo ?: UserInfo(name = "")) { userInfo ->
        // weed to process user names, as some authorization providers pass emails instead of names
        val atIndex = userInfo.name.indexOf('@')
        val processedName = if (atIndex >= 0) userInfo.name.substring(0, atIndex) else userInfo.name
        userInfo.copy(name = processedName)
    }

    val navigate = useNavigate()

    val saveUser = useDeferredRequest {
        val newUserInfo = userInfo.copy(
            oldName = props.userInfo?.name!!,
            status = UserStatus.ACTIVE,
        )
        val response = post(
            "$apiUrl/users/save",
            jsonHeaders,
            Json.encodeToString(newUserInfo),
            loadingHandler = ::loadingHandler,
            responseHandler = ::responseHandlerWithValidation,
        )
        if (response.ok) {
            window.location.reload()
        } else if (response.isConflict()) {
            setConflictErrorMessage(response.unpackMessage())
        }
    }

    val logOut = useDeferredRequest {
        val replyToLogout = post(
            "${window.location.origin}/logout",
            Headers(),
            "ping",
            loadingHandler = ::loadingHandler,
        )
        if (replyToLogout.ok) {
            window.location.href = window.location.origin
            window.location.reload()
        }
    }

    val (newAvatar, setNewAvatar) = useState<File?>(null)
    useRequest(dependencies = arrayOf(newAvatar)) {
        newAvatar?.let { avatar ->
            postImageUpload(
                avatar,
                props.userInfo?.name!!,
                AvatarType.USER,
                loadingHandler = ::loadingHandler,
            )
        }
    }

    if (props.userInfo?.status == UserStatus.ACTIVE) {
        navigate("/", jso { replace = true })
    }

    main {
        className = ClassName("main-content mt-0 ps")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            span {
                className = ClassName("mask bg-gradient-dark opacity-6")
            }
            div {
                className = ClassName("row justify-content-center")
                div {
                    className = ClassName("col-sm-4")
                    div {
                        className = ClassName("container card o-hidden border-0 shadow-lg my-2 card-body p-0")
                        style = jso {
                            zIndex = (MAX_Z_INDEX - 1).unsafeCast<ZIndex>()
                        }
                        div {
                            className = ClassName("p-5 text-center")

                            h1 {
                                className = ClassName("h4 text-gray-900 mb-4")
                                +"Set your user name and avatar"
                            }

                            renderAvatar(props.userInfo?.avatar) { setNewAvatar(it) }

                            form {
                                div {
                                    inputTextFormRequired {
                                        form = InputTypes.USER_NAME
                                        textValue = userInfo.name
                                        validInput = userInfo.name.isNotEmpty() && userInfo.name.isValidName() && userInfo.name.isValidLengthName()
                                        classes = ""
                                        name = "User name"
                                        conflictMessage = conflictErrorMessage
                                        onChangeFun = { event ->
                                            setUserInfo { previousUserInfo -> previousUserInfo.copy(name = event.target.value) }
                                            setConflictErrorMessage(null)
                                        }
                                    }
                                }

                                div {
                                    className = ClassName("mt-2 form-check row")
                                    input {
                                        className = ClassName("form-check-input")
                                        type = "checkbox".unsafeCast<InputType>()
                                        value = ""
                                        id = "terms-of-use"
                                        onChange = { setIsTermsOfUseOk(it.target.checked) }
                                    }
                                    label {
                                        className = ClassName("form-check-label")
                                        +" I agree with "
                                        Link {
                                            to = "/${FrontendRoutes.TERMS_OF_USE}"
                                            target = "_blank".unsafeCast<WindowTarget>()
                                            +"terms of use"
                                        }
                                    }
                                }

                                buttonBuilder(
                                    "Sign up",
                                    "info",
                                    classes = "mt-4 mr-4",
                                    isDisabled = !isTermsOfUseOk,
                                ) { saveUser() }

                                buttonBuilder(
                                    "Log out",
                                    "danger",
                                    classes = "mt-4",
                                ) { logOut() }

                                conflictErrorMessage?.let {
                                    div {
                                        className = ClassName("invalid-feedback d-block")
                                        +it
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

/**
 * `Props` retrieved from router
 */
external interface RegistrationProps : PropsWithChildren {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?
}

@Suppress("MAGIC_NUMBER")
private fun ChildrenBuilder.renderAvatar(avatar: String?, onAvatarUpload: (File) -> Unit) {
    label {
        className = ClassName("btn")
        title = "Change the user's avatar"
        input {
            type = InputType.file
            hidden = true
            onChange = { event ->
                val file = event.target.files!!.asList()
                    .single()
                onAvatarUpload(file)
            }
        }
        img {
            className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
            src = avatar?.let { "/api/$v1/avatar$it" } ?: AVATAR_PROFILE_PLACEHOLDER
            style = jso {
                height = 16.rem
                width = 16.rem
            }
        }
    }
}
