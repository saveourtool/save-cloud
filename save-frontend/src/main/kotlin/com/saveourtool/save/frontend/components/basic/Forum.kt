@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.CommentDto
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.externals.fontawesome.faPaperPlane
import com.saveourtool.save.frontend.externals.fontawesome.faTimes
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.AVATAR_PLACEHOLDER
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.toUnixCalendarFormat
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes

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
import react.router.dom.Link
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
        className = ClassName("shadow col mx-auto border border-secondary card card-body p-0")
        div {
            className = ClassName("row no-gutters mx-auto input-group px-0 shadow-none")
            renderLeftColumn(
                props.currentUserInfo.avatar,
                props.currentUserInfo.name,
                props.currentUserInfo.rating,
                "#e1e9ed"
            )
            div {
                className = ClassName("col")
                textarea {
                    className = ClassName("form-control p-3 border-0")
                    style = jso {
                        width = "100%".unsafeCast<Width>()
                        height = "100%".unsafeCast<Height>()
                    }
                    onChange = { event -> setComment { it.copy(message = event.target.value) } }
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
            className = ClassName("d-flex justify-content-end p-2")
            style = jso { background = "#e1e9ed".unsafeCast<Background>() }
            buttonBuilder(
                faPaperPlane,
                isDisabled = comment.message.isBlank(),
                classes = "rounded-circle btn-sm",
                isOutline = true,
            ) {
                enrollRequest()
            }
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
            className = ClassName("shadow-none card col-10 text-left border-0")
            val comment = props.comment
            div {
                className = ClassName("flex-wrap d-flex justify-content-between")
                style = jso { background = "#f1f1f1".unsafeCast<Background>() }
                span {
                    className = ClassName("ml-1")
                    +(comment.createDate?.toUnixCalendarFormat(TimeZone.currentSystemDefault()) ?: "Unknown")
                }
                div {
                    if (props.currentUserInfo?.canDelete(props.comment) == true) {
                        buttonBuilder(faTimes, style = "", classes = "btn-sm") {
                            if (window.confirm("Are you sure you want to delete a comment?")) {
                                props.setCommentForDeletion(props.comment)
                            }
                        }
                    }
                }
            }
            div {
                className = ClassName("shadow-none card card-body border-0")
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

    /**
     * [UserInfo] of current user
     */
    var currentUserInfo: UserInfo?

    /**
     * Callback invoked to set selected comment for deletion
     */
    var setCommentForDeletion: (CommentDto) -> Unit
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
    var currentUserInfo: UserInfo
}

private fun UserInfo.canDelete(commentDto: CommentDto) = isSuperAdmin() || name == commentDto.userName

@Suppress("MAGIC_NUMBER")
private fun ChildrenBuilder.renderLeftColumn(
    userAvatar: String?,
    name: String,
    rating: Long,
    color: String = "#f1f1f1",
) {
    val (avatar, setAvatar) = useState(userAvatar?.let { "/api/$v1/avatar$it" } ?: AVATAR_PROFILE_PLACEHOLDER)

    div {
        className = ClassName("input-group-prepend col-2")
        style = jso {
            background = color.unsafeCast<Background>()
        }
        div {
            className = ClassName("mb-0 font-weight-bold text-gray-800")
            form {
                div {
                    className = ClassName("row justify-content-center g-3 ml-3 mr-3 pb-2 pt-2 border-bottom-0")
                    div {
                        className = ClassName("md-4 pl-0 pr-0")
                        Link {
                            img {
                                className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                src = avatar
                                height = 80.0
                                width = 80.0
                                onError = { setAvatar(AVATAR_PLACEHOLDER) }
                            }
                            to = "/${FrontendRoutes.PROFILE.path}/$name"
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
                            +rating.toString()
                        }
                        h1 {
                            className = ClassName("col-12 text-center font-weight-bold h5")
                            Link {
                                to = "/${FrontendRoutes.PROFILE.path}/$name"
                                +name
                            }
                        }
                    }
                }
            }
        }
    }
}
