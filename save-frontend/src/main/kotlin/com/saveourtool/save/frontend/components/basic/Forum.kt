@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.CommentDto
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.externals.fontawesome.faPaperPlane
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.AVATAR_PLACEHOLDER
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.toUnixCalendarFormat
import com.saveourtool.save.v1

import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.PropsWithChildren
import react.dom.aria.ariaDescribedBy
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.textarea
import react.useState
import web.cssom.*

import kotlinx.browser.window
import kotlinx.datetime.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @return a function component
 */
@Suppress(
    "GENERIC_VARIABLE_WRONG_DECLARATION",
    "MAGIC_NUMBER",
)
val newCommentWindow: FC<NewCommentWindowProps> = FC { props ->
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
            props.addComment()
            setComment(CommentDto.empty)
        }
    }

    div {
        className = ClassName("shadow row no-gutters mx-auto input-group border border-secondary")
        renderLeftColumn(
            props.currentUserInfo?.avatar,
            props.currentUserInfo?.name,
            props.currentUserInfo?.rating,
            "#ceedc7"
        )
        div {
            className = ClassName("col shadow")
            textarea {
                className = ClassName("form-control p-3 border-0")
                style = jso {
                    width = "100%".unsafeCast<Width>()
                    height = "100%".unsafeCast<Height>()
                }
                onChange = { event ->
                    setComment { it.copy(message = event.target.value) }
                }
                value = comment.message
                ariaDescribedBy = "${InputTypes.COMMENT.name}Span"
                rows = 5
                id = InputTypes.COMMENT.name
                required = true
                placeholder = "Write a comment"
            }
        }
    }
    div {
        className = ClassName("d-flex justify-content-end")
        buttonBuilder(faPaperPlane, isDisabled = comment.message.isBlank(), classes = "mt-3") {
            enrollRequest()
        }
    }
}

/**
 * [FC] for comment displaying
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION", "MAGIC_NUMBER")
val commentWindow: FC<CommentWindowProps> = FC { props ->
    div {
        className = ClassName("shadow input-group row no-gutters mx-auto border-secondary")
        renderLeftColumn(
            props.comment.userAvatar,
            props.comment.userName,
            props.comment.userRating,
        )
        div {
            className = ClassName("card col-10 text-left border-0")
            val comment = props.comment
            div {
                className = ClassName("flex-wrap")
                style = jso { background = "#F1F1F1".unsafeCast<Background>() }
                span {
                    className = ClassName("ml-1")
                    +(comment.createDate?.toUnixCalendarFormat(TimeZone.currentSystemDefault()) ?: "Unknown")
                }
            }
            div {
                className = ClassName("card card-body border-0")
                markdown(comment.message.split("\n").joinToString("\n\n"))
            }
        }
    }
}

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
 * Props for new comment card component
 */
external interface NewCommentWindowProps : PropsWithChildren {
    /**
     * Callback invoked when added new comment
     */
    var addComment: () -> Unit

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo?
}

@Suppress(
    "MAGIC_NUMBER",
)
private fun ChildrenBuilder.renderLeftColumn(
    userAvatar: String?,
    name: String?,
    rating: Long?,
    color: String = "#e1e9ed",
) {
    val (avatar, setAvatar) = useState(userAvatar?.let { "/api/$v1/avatar$it" } ?: "img/undraw_profile.svg")

    div {
        className = ClassName("input-group-prepend col-2")
        style = jso {
            background = color.unsafeCast<Background>()
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
                        className = ClassName("row mt-2 md-6 pl-0")
                        style = jso {
                            display = Display.flex
                            alignItems = AlignItems.center
                        }
                        div {
                            className = ClassName("col-12 text-center text-xs font-weight-bold text-info text-uppercase")
                            +"Rating"
                        }
                        div {
                            className = ClassName("col-12 text-center text-xs")
                            +(rating ?: 0L).toString()
                        }
                        h1 {
                            className = ClassName("col-12 text-center font-weight-bold h5")
                            +name
                        }
                    }
                }
            }
        }
    }
}
