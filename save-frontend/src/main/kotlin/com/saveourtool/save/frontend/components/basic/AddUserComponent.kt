@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.components.inputform.*
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.DATABASE_DELIMITER
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.useState
import web.cssom.ClassName

/**
 * Component for adding user to some group
 */
val addUserComponent: FC<AddUserComponentProps> = FC { props ->
    val (user, setUser) = useState(UserInfo(name = "", source = ""))

    div {
        className = ClassName("")
        inputWithDebounceForUserInfo {
            selectedOption = user
            setSelectedOption = { setUser(it) }
            placeholder = "Input name..."
            renderOption = ::renderUserWithAvatar
            onOptionClick = props.onUserAdd
            maxOptions = 2
            getUrlForOptionsFetch = { prefix ->
                props.idsToSkip.joinToString(DATABASE_DELIMITER)
                    .let { ids -> if (ids.isNotBlank()) "&ids=$ids" else "" }
                    .let { ids -> "$apiUrl/users/by-prefix/?prefix=$prefix$ids" }
            }
        }
    }
}

/**
 * [Props] for [addUserComponent]
 */
external interface AddUserComponentProps : Props {
    /**
     * Callback invoked on user click
     */
    var onUserAdd: (UserInfo) -> Unit

    /**
     * [Set] of ids to ignore when showing results
     */
    var idsToSkip: Set<Long>
}
