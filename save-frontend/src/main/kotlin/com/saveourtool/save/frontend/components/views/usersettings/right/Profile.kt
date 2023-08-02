/**
 * rendering for Profile management card
 */

@file:Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")

package com.saveourtool.save.frontend.components.views.usersettings.right

import com.saveourtool.save.frontend.components.basic.avatarForm
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.views.usersettings.AVATAR_TITLE
import com.saveourtool.save.frontend.components.views.usersettings.SettingsProps
import com.saveourtool.save.frontend.components.views.usersettings.inputForm
import com.saveourtool.save.frontend.components.views.usersettings.saveUser
import com.saveourtool.save.frontend.http.postImageUpload
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.v1
import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.StateSetter
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.textarea
import react.useState
import web.cssom.ClassName
import web.cssom.rem

const val AVATARS_PACKAGE_COUNT = 9

val profile: FC<SettingsProps> = FC { props ->
    // === states ===
    val (isAvatarWindowOpen, setIsAvatarWindowOpen) = useState(false)
    val (avatarImgLink, setAvatarImgLink) = useState<String?>(null)
    val (fieldsMap, setFieldsMap) =
            useState<MutableMap<InputTypes, String?>>(mutableMapOf())
    val (fieldsValidationMap, setfieldsValidationMap) =
            useState<MutableMap<InputTypes, String?>>(mutableMapOf())
    val saveUser = saveUser(fieldsMap, props, fieldsValidationMap, setfieldsValidationMap)

    // === image editor ===
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

    // === design ===
    div {
        className = ClassName("row px-5 mt-3")

        hr { }

        div {
            className = ClassName("col-6 text-center")
            div {
                className = ClassName("row")
                h4 {
                    +"Add bio and info:"
                }
            }
            div {
                className = ClassName("row pr-5")
                div {
                    className = ClassName("input-group needs-validation")
                    textarea {
                        className = ClassName("form-control")
                        onChange = {
                            fieldsMap[InputTypes.FREE_TEXT] = it.target.value
                            setFieldsMap(fieldsMap)
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
                className = ClassName("row")
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
                            props,
                            avatarImgLink,
                            setIsAvatarWindowOpen,
                            setAvatarImgLink,
                            "/img/upload_avatar.png"
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
                                renderPreparedAvatars(lowerBound..i)
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
            extraInformation(props, fieldsMap, fieldsValidationMap, setFieldsMap)

            div {
                className = ClassName("row justify-content-center")
                buttonBuilder("Save changes", style = "primary") {
                    saveUser()
                }
            }
        }
    }
}

typealias FieldsStateSetter = StateSetter<MutableMap<InputTypes, String?>>

/**
 * @param props
 * @param avatarImgLink
 * @param setIsAvatarWindowOpen
 * @param setAvatarImgLink
 * @param placeholder
 */
internal fun ChildrenBuilder.avatarEditor(
    props: SettingsProps,
    avatarImgLink: String?,
    setIsAvatarWindowOpen: StateSetter<Boolean>,
    setAvatarImgLink: StateSetter<String?>,
    placeholder: String,
) {
    label {
        className = ClassName("btn animated-provider")
        title = AVATAR_TITLE
        onClick = {
            setIsAvatarWindowOpen(true)
        }
        img {
            className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
            src = avatarImgLink
                ?: props.userInfo?.avatar?.let { "/api/$v1/avatar$it" }
                ?: placeholder
            style = jso {
                height = 8.rem
                width = 8.rem
            }
            onError = {
                setAvatarImgLink(AVATAR_PLACEHOLDER)
            }
        }
    }
}

private fun ChildrenBuilder.extraInformation(
    props: SettingsProps,
    fieldsMap: MutableMap<InputTypes, String?>,
    fieldsValidationMap: MutableMap<InputTypes, String?>,
    setFieldsMap: FieldsStateSetter
) {
    hr { }

    inputForm(
        props.userInfo?.realName,
        InputTypes.REAL_NAME,
        fieldsMap,
        fieldsValidationMap,
        setFieldsMap,
        "e.g. John Smith"
    )
    inputForm(
        props.userInfo?.company,
        InputTypes.COMPANY,
        fieldsMap,
        fieldsValidationMap,
        setFieldsMap,
        "e.g. FutureWay Inc."
    )
    inputForm(
        props.userInfo?.location,
        InputTypes.LOCATION,
        fieldsMap,
        fieldsValidationMap,
        setFieldsMap,
        "Beijing, China"
    )
    inputForm(
        props.userInfo?.website,
        InputTypes.WEBSITE,
        fieldsMap,
        fieldsValidationMap,
        setFieldsMap,
        "https://saveourtool.com"
    )
    inputForm(props.userInfo?.linkedin, InputTypes.LINKEDIN, fieldsMap, fieldsValidationMap, setFieldsMap)
    inputForm(props.userInfo?.gitHub, InputTypes.GITHUB, fieldsMap, fieldsValidationMap, setFieldsMap)
    inputForm(props.userInfo?.twitter, InputTypes.TWITTER, fieldsMap, fieldsValidationMap, setFieldsMap)

    hr { }
}

private fun ChildrenBuilder.renderPreparedAvatars(avatarsRange: IntRange) {
    for (i in avatarsRange) {
        img {
            className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
            src = "/img/avatar_packs/avatar$i.png"
            style = jso {
                height = 5.1.rem
                width = 5.1.rem
            }
        }
    }
}
