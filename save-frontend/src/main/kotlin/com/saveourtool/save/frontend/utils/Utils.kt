/**
 * Various utils for frontend
 */

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.Role.SUPER_ADMIN
import com.saveourtool.save.frontend.common.utils.WithRequestStatusContext
import com.saveourtool.save.info.UserInfo

import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.xhr.FormData
import react.ChildrenBuilder
import react.StateSetter
import react.dom.events.ChangeEvent
import react.dom.events.MouseEventHandler
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.samp
import react.dom.html.ReactHTML.small
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.tr
import web.cssom.ClassName
import web.dom.Element
import web.html.HTMLInputElement

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Avatar placeholder if an error was thrown.
 */
internal const val AVATAR_PLACEHOLDER = "/img/undraw_image_not_found.png"

/**
 * Avatar profile for those who don't want to upload it
 */
internal const val AVATAR_PROFILE_PLACEHOLDER = "/img/avatar_placeholder.png"

/**
 * Timeout after `onBlur` event takes place but before the component is hidden
 */
const val ON_BLUR_TIMEOUT_MILLIS = 200

/**
 * The body of a [useDeferredRequest] invocation.
 *
 * @param T the return type of this action.
 */
internal typealias DeferredRequestAction<T> = suspend (WithRequestStatusContext) -> T

/**
 * Append an object [obj] to `this` [FormData] as a JSON, using kx.serialization for serialization
 *
 * @param name key to be appended to the form data
 * @param obj an object to be appended
 * @return Unit
 */
inline fun <reified T> FormData.appendJson(name: String, obj: T) =
        append(
            name,
            Blob(
                arrayOf(Json.encodeToString(obj)),
                BlobPropertyBag("application/json")
            )
        )

/**
 * @return [Role] if string matches any role, else throws [IllegalStateException]
 * @throws IllegalStateException if string is not matched with any role
 */
fun String.toRole() = Role.values().find {
    this == it.formattedName || this == it.toString()
} ?: throw IllegalStateException("Unknown role is passed: $this")

/**
 * @return lambda which does the same as receiver but takes unused arg
 */
fun <T> (() -> Unit).withUnusedArg(): (T) -> Unit = { this() }

/**
 * Converts `this` no-argument function to a [MouseEventHandler].
 *
 * @return `this` function as a [MouseEventHandler].
 * @see MouseEventHandler
 */
fun <T : Element> (() -> Unit).asMouseEventHandler(): MouseEventHandler<T> =
        {
            this()
        }

/**
 * @return lambda which does the same but take value from [HTMLInputElement]
 */
fun StateSetter<String?>.fromInput(): (ChangeEvent<HTMLInputElement>) -> Unit =
        { event -> this(event.target.value) }

/**
 * @return lambda which does the same but take value from [HTMLInputElement]
 */
fun StateSetter<String>.fromInput(): (ChangeEvent<HTMLInputElement>) -> Unit =
        { event -> this(event.target.value) }

/**
 * Parse string in format
 *
 * FILE (START_ROW:START_COL-END_ROW:END_COL)
 *
 * into [[START_ROW, START_COL], [END_ROW, END_COL]]
 *
 * @return list in format: [[START_ROW, START_COL], [END_ROW, END_COL]]
 */
@Suppress("MAGIC_NUMBER")
fun String.parsePositionString(): List<Int>? = substringAfter("(", "")
    .substringBefore(")", "")
    .split("-")
    .map { positionList -> positionList.split(":") }
    .flatten()
    .takeIf { it.size == 4 }
    ?.mapIndexed { idx, value -> value.toInt() - idx % 2 }

/**
 * @param time time to set to [LocalDateTime]
 * @return [LocalDateTime] from [String]
 */
fun String.dateStringToLocalDateTime(time: LocalTime = LocalTime(0, 0, 0)) = LocalDateTime(
    LocalDate.parse(this),
    time,
)

/**
 * @return `true` if this user is a super-admin, `false` otherwise.
 * @see Role.isSuperAdmin
 */
internal fun UserInfo?.isSuperAdmin(): Boolean = this?.globalRole.isSuperAdmin()

/**
 * @return `true` if this is a super-admin role, `false` otherwise.
 * @see UserInfo.isSuperAdmin
 */
internal fun Role?.isSuperAdmin(): Boolean = this?.isHigherOrEqualThan(SUPER_ADMIN) ?: false

/**
 * Adds this text to ChildrenBuilder line by line, separating with `<br>`
 *
 * @param text text to display
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
internal fun ChildrenBuilder.multilineText(text: String) {
    text.lines().forEach {
        small {
            samp {
                +it
            }
        }
        br { }
    }
}

/**
 * @param text
 */
internal fun ChildrenBuilder.multilineTextWithIndices(text: String) {
    table {
        className = ClassName("table table-borderless table-hover table-sm")
        tbody {
            text.lines().filterNot { it.isEmpty() }.forEachIndexed { i, line ->
                tr {
                    td {
                        +"${i + 1}"
                    }
                    td {
                        +line
                    }
                }
            }
        }
    }
}

/**
 * @param maxLength
 * @return true if string is invalid
 */
internal fun String?.isInvalid(maxLength: Int) = this.isNullOrBlank() || this.contains(" ") || this.length > maxLength

/**
 * @param digits number of digits to round to
 */
internal fun Double.toFixed(digits: Int) = asDynamic().toFixed(digits)

/**
 * @param digits number of digits to round to
 * @return rounded value as String
 */
internal fun Double.toFixedStr(digits: Int) = toFixed(digits).toString()
