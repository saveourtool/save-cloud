@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.CommentDto
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.AVATAR_PLACEHOLDER
import com.saveourtool.save.utils.toUnixCalendarFormat
import com.saveourtool.save.v1

import js.core.jso
import react.FC
import react.PropsWithChildren
import react.dom.aria.ariaDescribedBy
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.textarea
import react.useState
import web.cssom.*

import kotlinx.browser.window
import kotlinx.datetime.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Props for comment card component
 */
external interface CommentWindowProps : PropsWithChildren {
    /**
     * User comment
     */
    var comment: CommentDto
}

/**
 * @return a function component
 */
fun newCommentWindow() = FC<PropsWithChildren> {
    val (comment, setComment) = useState(CommentDto.empty)

    val enrollRequest = useDeferredRequest {
        val commentNew = comment.copy(section = window.location.hash)
        val response = post(
            url = "$apiUrl/comments/save",
            headers = jsonHeaders,
            body = Json.encodeToString(commentNew),
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )
        if (response.ok) {
            window.location.reload()
        }
    }

    div {
        className = ClassName("input-group needs-validation")
        textarea {
            className = ClassName("form-control")
            onChange = { event ->
                setComment { it.copy(message = event.target.value) }
            }
            ariaDescribedBy = "${InputTypes.COMMENT.name}Span"
            rows = 5
            id = InputTypes.COMMENT.name
            required = true
        }
    }
    div {
        className = ClassName("d-flex justify-content-end")
        buttonBuilder(label = "Comment") {
            enrollRequest()
        }
    }
}

/**
 * @return a function component
 */
@Suppress("TOO_LONG_FUNCTION")
fun commentWindow() = FC<CommentWindowProps> { props ->

    val columnCard = cardComponent(isBordered = false, hasBg = true, isNoPadding = false, isPaddingBottomNull = true, isFilling = true)
    val (avatar, setAvatar) = useState(props.comment.userAvatar?.let { "/api/$v1/avatar$it" } ?: "img/undraw_profile.svg")

    div {
        className = ClassName("row no-gutters mx-auto border-secondary")
        div {
            className = ClassName("card card-body col-2 border-secondary")
            style = jso {
                background = "#e1e9ed".unsafeCast<Background>()
            }
            div {
                className = ClassName("mb-0 font-weight-bold text-gray-800")
                form {
                    div {
                        className = ClassName("row justify-content-center g-3 ml-3 mr-3 pb-2 pt-2 border-bottom")
                        div {
                            className = ClassName("md-4 pl-0 pr-0")
                            img {
                                className =
                                        ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                src = avatar
                                height = 80.0
                                width = 80.0
                                onError = {
                                    setAvatar(AVATAR_PLACEHOLDER)
                                }
                            }
                        }
                        div {
                            className = ClassName("mt-2 md-6 pl-0")
                            style = jso {
                                display = Display.flex
                                alignItems = AlignItems.center
                            }
                            h1 {
                                className = ClassName("h5 mb-0 text-gray-800")
                                +props.comment.userName
                            }
                        }
                    }
                }
            }
        }
        div {
            className = ClassName("card col-10 text-left border-secondary")
            val comment = props.comment
            div {
                className = ClassName("col")
                style = jso {
                    background = "#F1F1F1".unsafeCast<Background>()
                }
                +(comment.createDate?.toUnixCalendarFormat(TimeZone.currentSystemDefault()) ?: "Unknown")
            }
            columnCard {
                markdown(comment.message.split("\n").joinToString("\n\n"))
            }
        }
    }
}
