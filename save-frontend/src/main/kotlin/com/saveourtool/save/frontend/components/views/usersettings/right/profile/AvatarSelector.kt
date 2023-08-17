package com.saveourtool.save.frontend.components.views.usersettings.right.profile

import com.saveourtool.save.frontend.components.basic.avatarForm
import com.saveourtool.save.frontend.components.views.usersettings.AVATAR_TITLE
import com.saveourtool.save.frontend.externals.fontawesome.faCamera
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.AVATARS_PACKS_DIR
import com.saveourtool.save.utils.CONTENT_LENGTH_CUSTOM
import com.saveourtool.save.utils.FILE_PART_NAME
import com.saveourtool.save.utils.AvatarType.USER

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML
import react.router.dom.Link
import web.cssom.*
import web.file.File
import web.http.FormData

import kotlinx.browser.window
import kotlinx.coroutines.await

val avatarSelector = FC<AvatarSelectorProps> { props ->
    val (avatar, setAvatar) = useState<File?>(null)
    val (selectedAvatar, setSelectedAvatar) = useState("")
    val avatarWindowOpen = useWindowOpenness()

    val setAvatarFromResources = useDeferredRequest {
        val response = get(
            url = "$apiUrl/avatar/avatar_update",
            params = jso<dynamic> {
                this.type = USER
                this.resource = selectedAvatar
            },
            jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            props.userInfoSetter(props.userInfo?.copy(avatar = selectedAvatar))
        }
    }

    val saveAvatar = useDeferredRequest {
        avatar?.let {
            val response = post(
                url = "$apiUrl/avatar/upload",
                params = jso<dynamic> {
                    this.owner = props.userInfo?.name
                    this.type = USER
                },
                Headers().apply { append(CONTENT_LENGTH_CUSTOM, avatar.size.toString()) },
                FormData().apply { set(FILE_PART_NAME, avatar) },
                loadingHandler = ::loadingHandler,
            )
            val text = response.text().await()
            if (response.ok) {
               props.userInfoSetter(props.userInfo?.copy(avatar = text))
            }
        }
    }

    // === image editor ===
    avatarForm {
        isOpen = avatarWindowOpen.isOpen()
        title = AVATAR_TITLE
        onCloseWindow = {
            saveAvatar()
            avatarWindowOpen.closeWindow()
        }
        imageUpload = { file ->
            setAvatar(file)
        }
    }

    ReactHTML.div {
        className = ClassName("row")
        ReactHTML.div {
            className = ClassName("col-4")
            ReactHTML.div {
                className = ClassName("row")
                avatarEditor(
                    avatarWindowOpen,
                )
            }
        }
        ReactHTML.div {
            className = ClassName("col-8")
            // render prepared preselected avatars 3 in row
            var lowerBound = 1
            for (i in 1..AVATARS_PACKAGE_COUNT) {
                if (i % 3 == 0) {
                    ReactHTML.div {
                        className = ClassName("row")
                        for (j in lowerBound..i) {
                            val newAvatar = "$AVATARS_PACKS_DIR/avatar$j.png"
                            ReactHTML.div {
                                className = ClassName("animated-provider")
                                ReactHTML.img {
                                    className =
                                            ClassName("avatar avatar-user width-full border color-bg-default rounded-circle shadow")
                                    src = newAvatar
                                    style = jso {
                                        height = 5.1.rem
                                        width = 5.1.rem
                                        cursor = Cursor.pointer
                                    }
                                    onClick = {
                                        setSelectedAvatar(newAvatar)
                                        setAvatarFromResources()
                                    }
                                }
                            }
                        }
                        lowerBound = i + 1
                    }
                }
            }
        }
    }
}

external interface AvatarSelectorProps : PropsWithChildren {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?

    /**
     * After updating user information we will update userSettings without re-rendering the page
     * PLEASE NOTE: THIS PROPERTY AFFECTS RENDERING OF WHOLE APP.KT
     * IF YOU HAVE SOME PROBLEMS WITH IT, CHECK THAT YOU HAVE PROPAGATED IT PROPERLY:
     * { this.userInfoSetter = (!) PROPS (!) .userInfoSetter }
     */
    var userInfoSetter: StateSetter<UserInfo?>
}

/**
 * @param avatarWindowOpen
 */
internal fun ChildrenBuilder.avatarEditor(
    avatarWindowOpen: WindowOpenness,
) {
    Link {
        className = ClassName("btn px-0 pt-0")
        title = AVATAR_TITLE
        onClick = {
            avatarWindowOpen.openWindow()
        }
        ReactHTML.div {
            className = ClassName("card card-body shadow")
            style = jso {
                display = Display.flex
                justifyContent = JustifyContent.center
                height = 8.rem
                width = 8.rem
            }
            ReactHTML.div {
                className = ClassName("row justify-content-center")
                fontAwesomeIcon(faCamera, classes = "fa-xl")
            }
            ReactHTML.div {
                className = ClassName("row mt-2 justify-content-center")
                +AVATAR_TITLE
            }
        }
    }
}
