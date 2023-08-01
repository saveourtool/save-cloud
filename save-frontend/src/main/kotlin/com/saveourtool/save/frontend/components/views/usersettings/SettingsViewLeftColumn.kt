package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.basic.avatarForm
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.http.postImageUpload
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.AVATAR_PLACEHOLDER
import com.saveourtool.save.frontend.utils.AVATAR_PROFILE_PLACEHOLDER
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.VFC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.nav
import react.router.dom.Link
import react.useState
import web.cssom.Background
import web.cssom.ClassName
import web.cssom.rem

private const val AVATAR_TITLE = "Update avatar"

val leftColumn = FC<SettingsProps> { props ->

    val (isAvatarWindowOpen, setIsAvatarWindowOpen) = useState(false)
    val (avatarImgLink, setAvatarImgLink) = useState<String?>(null)

    // avatar editor form
    avatarForm {
        isOpen = isAvatarWindowOpen
        title = AVATAR_TITLE
        onCloseWindow = {
            setIsAvatarWindowOpen(false)
        }
        imageUpload = { file ->
            useRequest {
                postImageUpload(file, props.userInfo?.name, AvatarType.USER, ::loadingHandler)
            }
        }
    }

    div {
        className = ClassName("card card-body pt-0 px-0")
        style = cardHeight
        div {
            className = ClassName("col mr-2 px-0")
            style = jso {
                background = "#e1e9ed".unsafeCast<Background>()
            }
            div {
                className = ClassName("mb-0 font-weight-bold text-gray-800")
                form {
                    div {
                        className = ClassName("row g-3 ml-3 mr-3 pb-2 pt-2 border-bottom")
                        div {
                            className = ClassName("col")
                            div {
                                className = ClassName("row justify-content-center")
                                label {
                                    className = ClassName("btn")
                                    title = AVATAR_TITLE
                                    onClick = {
                                        setIsAvatarWindowOpen(true)
                                    }
                                    img {
                                        className =
                                                ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                        src = avatarImgLink
                                            ?: props.userInfo?.avatar?.let { "/api/$v1/avatar$it" }
                                            ?: AVATAR_PROFILE_PLACEHOLDER
                                        style = jso {
                                            height = 10.rem
                                            width = 10.rem
                                        }
                                        onError = {
                                            setAvatarImgLink(AVATAR_PLACEHOLDER)
                                        }
                                    }
                                }
                            }
                            div {
                                className = ClassName("row justify-content-center")
                                h4 {
                                    className = ClassName("mb-0 text-gray-800")
                                    +(props.userInfo?.name ?: "")
                                }
                            }
                        }
                    }
                }
            }
        }
        settingsTabs {}
    }
}

val settingsTabs = VFC {
    div {
        className = ClassName("col mr-2 px-0")
        nav {
            div {
                className = ClassName("px-3 mt-3 ui vertical menu profile-setting")
                form {
                    settingMenuHeader("Basic Settings", "img/settings_icon1.png")
                    div {
                        className = ClassName("menu")
                        settingsMenuTab(FrontendRoutes.SETTINGS_PROFILE, "Profile settings", faUser)
                        settingsMenuTab(FrontendRoutes.SETTINGS_EMAIL, "Login and email", faEnvelope)
                        settingsMenuTab(FrontendRoutes.SETTINGS_ORGANIZATIONS, "Organizations", faCity)
                    }
                }
                form {
                    div {
                        className = ClassName("separator mt-3 mb-3")
                    }
                    settingMenuHeader("Security Settings", "img/settings_icon1.png")
                    div {
                        className = ClassName("menu")
                        settingsMenuTab(FrontendRoutes.SETTINGS_TOKEN, "Personal access tokens", faKey)
                        settingsMenuTab(FrontendRoutes.SETTINGS_TOKEN, "OAuth accounts", faGithub)
                    }
                }
                form {
                    div {
                        className = ClassName("separator mt-3 mb-3")
                    }
                    settingMenuHeader("Other", "img/settings_icon1.png")
                    div {
                        className = ClassName("menu")
                        settingsMenuTab(FrontendRoutes.SETTINGS_TOKEN, "Personal Statistics", faPlus)
                        settingsMenuTab(FrontendRoutes.SETTINGS_TOKEN, "Delete Profile", faWindowClose, "btn-outline-danger")
                    }
                }
            }
        }
    }
}

private fun ChildrenBuilder.settingMenuHeader(header: String, imgIcon: String) {
    div {
        className = ClassName("header")
        +header
    }
}

private fun ChildrenBuilder.settingsMenuTab(
    link: FrontendRoutes,
    text: String,
    icon: FontAwesomeIconModule,
    style: String = "btn-outline-dark"
) {
    div {
        className = ClassName("mt-2")
        Link {
            className = ClassName("btn $style btn-block text-left")
            to = "/${link.path}"
            fontAwesomeIcon(icon = icon) {
                it.className = "fas fa-sm fa-fw mr-2"
            }
            +text
        }
    }
}
