/**
 * rendering for Profile management card
 */

@file:Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")

package com.saveourtool.save.frontend.components.views.usersettings.right

import com.saveourtool.save.frontend.components.basic.avatarForm
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.views.usersettings.*
import com.saveourtool.save.frontend.components.views.usersettings.AVATAR_TITLE
import com.saveourtool.save.frontend.externals.fontawesome.faCamera
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.AVATARS_PACKS_DIR
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.CONTENT_LENGTH_CUSTOM
import com.saveourtool.save.utils.FILE_PART_NAME

import io.ktor.client.request.*
import js.core.jso
import org.w3c.fetch.Headers
import react.ChildrenBuilder
import react.FC
import react.StateSetter
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.textarea
import react.router.dom.Link
import react.useState
import web.cssom.*
import web.http.FormData

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val AVATARS_PACKAGE_COUNT = 9

val profileSettingsCard: FC<SettingsProps> = FC { props ->
    // === states ===
    val avatarWindowOpen = useWindowOpenness()
    val (settingsInputFields, setSettingsInputFields) = useState(SettingsInputFields())
    val (selectedAvatar, setSelectedAvatar) = useState<String>()
    val saveUser = saveUser(props, settingsInputFields, setSettingsInputFields)

    // === image editor ===
    avatarForm {
        isOpen = avatarWindowOpen.isOpen()
        title = AVATAR_TITLE
        onCloseWindow = {
            avatarWindowOpen.closeWindow()
        }
        imageUpload = { file ->
            // FixMe: we are inside of FC here, but without any reason avatarForm component is ignoring
            // FixMe: useRequest {} hook, only works with scope directly,
            // FixMe: expecting someone to investigate it later, but by for now we need this functionality to be working
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                val response = request(
                    url = "$apiUrl/avatar/upload".withParams(jso<dynamic> {
                        owner = props.userInfo?.name
                        this.type = AvatarType.USER
                    }),
                    method = "POST",
                    headers = Headers().apply { append(CONTENT_LENGTH_CUSTOM, file.size.toString()) },
                    body = FormData().apply { set(FILE_PART_NAME, file) },
                    loadingHandler = ::noopLoadingHandler,
                    responseHandler = ::noopResponseHandler,
                )
                if (response.ok) {
                    window.location.reload()
                }
            }
        }
    }

    // === design ===
    div {
        className = ClassName("row px-5 mt-3")

        hr { }

        div {
            className = ClassName("col-6 text-center")
            div {
                className = ClassName("row mb-2")
                h4 {
                    +"Add bio and info:"
                }
            }
            div {
                className = ClassName("row pr-5")
                div {
                    className = ClassName("input-group needs-validation")
                    textarea {
                        className = ClassName("form-control shadow")
                        onChange = {
                            val newSettingsInputFields =
                                    settingsInputFields.updateValue(InputTypes.FREE_TEXT, it.target.value, null)
                            setSettingsInputFields(newSettingsInputFields)
                        }
                        defaultValue = props.userInfo?.freeText
                        rows = 10
                    }
                }
            }
        }

        div {
            className = ClassName("col-6")
            div {
                className = ClassName("row mb-2")
                h4 {
                    +"Upload or select avatar:"
                }
            }

            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-4")
                    div {
                        className = ClassName("row")
                        avatarEditor(
                            avatarWindowOpen,
                        )
                    }
                }
                div {
                    className = ClassName("col-8")
                    // render prepared preselected avatars 3 in row
                    var lowerBound = 1
                    for (i in 1..AVATARS_PACKAGE_COUNT) {
                        if (i % 3 == 0) {
                            div {
                                className = ClassName("row")
                                renderPreparedAvatars(lowerBound..i, setSelectedAvatar)
                                lowerBound = i + 1
                            }
                        }
                    }
                }
            }
        }
    }

    div {
        className = ClassName("row")

        div {
            className = ClassName("col mt-2 px-5")
            extraInformation(props, settingsInputFields, setSettingsInputFields)

            div {
                className = ClassName("row justify-content-center")
                buttonBuilder("Save changes", style = "primary") {
                    saveUser()
                }
            }
        }
    }
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
        div {
            className = ClassName("card card-body shadow")
            style = jso {
                display = Display.flex
                justifyContent = JustifyContent.center
                height = 8.rem
                width = 8.rem
            }
            div {
                className = ClassName("row justify-content-center")
                fontAwesomeIcon(faCamera, classes = "fa-xl")
            }
            div {
                className = ClassName("row mt-2 justify-content-center")
                +AVATAR_TITLE
            }
        }
    }
}

private fun ChildrenBuilder.extraInformation(
    props: SettingsProps,
    settingsInputFields: SettingsInputFields,
    setSettingsInputFields: FieldsStateSetter
) {
    hr { }

    inputForm(
        props.userInfo?.realName,
        InputTypes.REAL_NAME,
        settingsInputFields,
        setSettingsInputFields,
        "e.g. John Smith"
    )
    inputForm(
        props.userInfo?.company,
        InputTypes.COMPANY,
        settingsInputFields,
        setSettingsInputFields,
        "e.g. FutureWay Inc."
    )
    inputForm(
        props.userInfo?.location,
        InputTypes.LOCATION,
        settingsInputFields,
        setSettingsInputFields,
        "Beijing, China"
    )
    inputForm(
        props.userInfo?.website,
        InputTypes.WEBSITE,
        settingsInputFields,
        setSettingsInputFields,
        "https://saveourtool.com"
    )
    inputForm(props.userInfo?.linkedin, InputTypes.LINKEDIN, settingsInputFields, setSettingsInputFields)
    inputForm(props.userInfo?.gitHub, InputTypes.GITHUB, settingsInputFields, setSettingsInputFields)
    inputForm(props.userInfo?.twitter, InputTypes.TWITTER, settingsInputFields, setSettingsInputFields)

    hr { }
}

private fun ChildrenBuilder.renderPreparedAvatars(avatarsRange: IntRange, setSelectedAvatar: StateSetter<String?>) {
    for (i in avatarsRange) {
        val avatar = "/img/$AVATARS_PACKS_DIR/avatar$i.png"
        div {
            className = ClassName("animated-provider")
            img {
                className =
                        ClassName("avatar avatar-user width-full border color-bg-default rounded-circle shadow")
                src = avatar
                style = jso {
                    height = 5.1.rem
                    width = 5.1.rem
                    cursor = Cursor.pointer
                }
                onClick = {
                    setSelectedAvatar(avatar)
                }
            }
        }
    }
}
