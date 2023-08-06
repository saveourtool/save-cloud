/**
 * A view with settings user
 */

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.entities.OrganizationWithUsers
import com.saveourtool.save.filters.OrganizationFilter
import com.saveourtool.save.frontend.components.basic.avatarForm
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.http.postImageUpload
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.events.ChangeEvent
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.nav
import react.router.dom.Link
import web.cssom.*
import web.html.HTMLInputElement

import kotlinx.browser.window
import kotlinx.coroutines.launch
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
    var userInfo: UserInfo?
}

/**
 * [State] of project view component
 */
@Suppress("MISSING_KDOC_TOP_LEVEL", "TYPE_ALIAS")
external interface UserSettingsViewState : State {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?

    /**
     * Token for user
     */
    var token: String?

    /**
     * A list of organization with users connected to user
     */
    var selfOrganizationWithUserList: List<OrganizationWithUsers>

    /**
     * Conflict error message
     */
    var conflictErrorMessage: String?

    /**
     * Flag to handle avatar Window
     */
    var isAvatarWindowOpen: Boolean

    /**
     * User avatar
     */
    var avatar: String
}

@Suppress("MISSING_KDOC_TOP_LEVEL")
abstract class UserSettingsView : AbstractView<UserSettingsProps, UserSettingsViewState>(Style.SAVE_LIGHT) {
    private val fieldsMap: MutableMap<InputTypes, String> = mutableMapOf()
    private val renderMenu = renderMenu()

    init {
        state.selfOrganizationWithUserList = emptyList()
        state.isAvatarWindowOpen = false
    }

    /**
     * @param fieldName
     * @param target
     */
    fun changeFields(
        fieldName: InputTypes,
        target: ChangeEvent<HTMLInputElement>,
    ) {
        val tg = target.target
        val value = tg.value
        fieldsMap[fieldName] = value
    }

    override fun componentDidMount() {
        super.componentDidMount()
        val comparator: Comparator<OrganizationWithUsers> =
                compareBy<OrganizationWithUsers> { it.organization.status.ordinal }
                    .thenBy { it.organization.name }

           scope.launch {
               val user = props.userInfo
               val organizationDtos = getOrganizationWithUsersList()
               setState {
                   userInfo = props.userInfo
                   userInfo?.let { updateFieldsMap(it) }
                   selfOrganizationWithUserList = organizationDtos.sortedWith(comparator)
                   avatar = user?.avatar?.let { "/api/$v1/avatar$it" } ?: AVATAR_PROFILE_PLACEHOLDER
               }
           }
    }

    private fun updateFieldsMap(userInfo: UserInfo) {
        userInfo.name.let { fieldsMap[InputTypes.USER_NAME] = it }
        userInfo.email?.let { fieldsMap[InputTypes.USER_EMAIL] = it }
        userInfo.company?.let { fieldsMap[InputTypes.COMPANY] = it }
        userInfo.location?.let { fieldsMap[InputTypes.LOCATION] = it }
        userInfo.linkedin?.let { fieldsMap[InputTypes.LINKEDIN] = it }
        userInfo.gitHub?.let { fieldsMap[InputTypes.GITHUB] = it }
        userInfo.twitter?.let { fieldsMap[InputTypes.TWITTER] = it }
    }

    /**
     * @return element
     */
    abstract fun renderMenu(): FC<UserSettingsProps>

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
    override fun ChildrenBuilder.render() {
        avatarForm {
            isOpen = state.isAvatarWindowOpen
            title = AVATAR_TITLE
            onCloseWindow = {
                setState {
                    isAvatarWindowOpen = false
                }
            }
            imageUpload = { file ->
                scope.launch {
                    postImageUpload(file, props.userInfo!!, AvatarType.USER, ::noopLoadingHandler)
                }
            }
        }

        div {
            className = ClassName("row justify-content-center")
            // ===================== LEFT COLUMN =======================================================================
            div {
                className = ClassName("col-2 mr-3")
                div {
                    className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0 border-secondary")
                    div {
                        className = ClassName("col mr-2 pr-0 pl-0")
                        style = jso {
                            background = "#e1e9ed".unsafeCast<Background>()
                        }
                        div {
                            className = ClassName("mb-0 font-weight-bold text-gray-800")
                            form {
                                div {
                                    className = ClassName("row g-3 ml-3 mr-3 pb-2 pt-2 border-bottom")
                                    div {
                                        className = ClassName("col-4 pl-0 pr-0")
                                        label {
                                            className = ClassName("btn")
                                            title = AVATAR_TITLE
                                            onClick = {
                                                setState {
                                                    isAvatarWindowOpen = true
                                                }
                                            }
                                            img {
                                                className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                                src = state.avatar
                                                height = 60.0
                                                width = 60.0
                                                onError = {
                                                    setState {
                                                        avatar = AVATAR_PLACEHOLDER
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    div {
                                        className = ClassName("col-6 pl-0")
                                        style = jso {
                                            display = Display.flex
                                            alignItems = AlignItems.center
                                        }
                                        h1 {
                                            className = ClassName("h5 mb-0 text-gray-800")
                                            +"${props.userInfo}"
                                        }
                                    }
                                }
                            }
                        }
                    }

                    div {
                        className = ClassName("col mr-2 pr-0 pl-0")
                        nav {
                            div {
                                className = ClassName("pl-3 ui vertical menu profile-setting")
                                form {
                                    div {
                                        className = ClassName("item mt-2")
                                        div {
                                            className = ClassName("header")
                                            +"Basic Setting"
                                        }
                                        div {
                                            className = ClassName("menu")
                                            div {
                                                className = ClassName("mt-2")
                                                Link {
                                                    className = ClassName("item")
                                                    to = "/${props.userInfo}/${FrontendRoutes.SETTINGS_PROFILE}"
                                                    fontAwesomeIcon(icon = faUser) {
                                                        it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                                                    }
                                                    +"Profile settings"
                                                }
                                            }
                                            div {
                                                className = ClassName("mt-2")
                                                Link {
                                                    className = ClassName("item")
                                                    to = "/${props.userInfo}/${FrontendRoutes.SETTINGS_EMAIL}"
                                                    fontAwesomeIcon(icon = faEnvelope) {
                                                        it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                                                    }
                                                    +"Email management"
                                                }
                                            }
                                            div {
                                                className = ClassName("mt-2")
                                                Link {
                                                    className = ClassName("item")
                                                    to = "/${props.userInfo}/${FrontendRoutes.SETTINGS_ORGANIZATIONS}"
                                                    fontAwesomeIcon(icon = faCity) {
                                                        it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                                                    }
                                                    +"Organizations"
                                                }
                                            }
                                        }
                                    }
                                }
                                form {
                                    div {
                                        className = ClassName("item mt-2")
                                        div {
                                            className = ClassName("header")
                                            +"Security Setting"
                                        }
                                        div {
                                            className = ClassName("menu")
                                            div {
                                                className = ClassName("mt-2")
                                                Link {
                                                    className = ClassName("item")
                                                    to = "/${props.userInfo}/${FrontendRoutes.SETTINGS_TOKEN}"
                                                    fontAwesomeIcon(icon = faKey) {
                                                        it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
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
            div {
                className = ClassName("col-6")
                renderMenu {
                    userInfo = props.userInfo
                }
            }
        }
    }

    @Suppress("MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
    fun updateUser() {
        val newName = fieldsMap[InputTypes.USER_NAME]?.trim()
        val nameInDb = state.userInfo!!.name
        val oldName = if (newName != nameInDb) nameInDb else null
        val newUserInfo = UserInfo(
            name = newName ?: nameInDb,
            oldName = oldName,
            originalLogins = state.userInfo!!.originalLogins,
            projects = state.userInfo!!.projects,
            email = fieldsMap[InputTypes.USER_EMAIL]?.trim(),
            company = fieldsMap[InputTypes.COMPANY]?.trim(),
            location = fieldsMap[InputTypes.LOCATION]?.trim(),
            linkedin = fieldsMap[InputTypes.LINKEDIN]?.trim(),
            gitHub = fieldsMap[InputTypes.GITHUB]?.trim(),
            twitter = fieldsMap[InputTypes.TWITTER]?.trim(),
            avatar = state.userInfo!!.avatar,
            status = state.userInfo!!.status,
        )

        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            val response = post(
                "$apiUrl/users/save",
                headers,
                Json.encodeToString(newUserInfo),
                loadingHandler = ::classLoadingHandler,
            )
            if (response.isConflict()) {
                val responseText = response.unpackMessage()
                setState {
                    conflictErrorMessage = responseText
                }
            } else {
                setState {
                    conflictErrorMessage = null
                }
            }
        }
    }

    @Suppress("MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
    fun deleteUser() {
        scope.launch {
            val response = get(
                url = "$apiUrl/users/delete/${state.userInfo!!.name}",
                headers = jsonHeaders,
                loadingHandler = ::classLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
            if (response.ok) {
                val replyToLogout = post(
                    "${window.location.origin}/logout",
                    Headers(),
                    "ping",
                    loadingHandler = ::classLoadingHandler,
                )
                if (replyToLogout.ok) {
                    window.location.href = "${window.location.origin}/#"
                    window.location.reload()
                }
            }
        }
    }

    @Suppress("TYPE_ALIAS")
    private suspend fun getOrganizationWithUsersList() = post(
        url = "$apiUrl/organizations/by-filters",
        headers = jsonHeaders,
        body = Json.encodeToString(OrganizationFilter.all),
        loadingHandler = ::classLoadingHandler,
    )
        .unsafeMap { it.decodeFromJsonString<List<OrganizationWithUsers>>() }

    companion object {
        private const val AVATAR_TITLE = "Change avatar owner"
    }
}
