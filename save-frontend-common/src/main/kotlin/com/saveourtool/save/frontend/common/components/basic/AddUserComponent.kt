@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.common.components.basic

import com.saveourtool.save.frontend.common.components.inputform.inputWithDebounceForUserInfo
import com.saveourtool.save.frontend.common.components.inputform.renderUserWithAvatar
import com.saveourtool.save.frontend.common.utils.apiUrl
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
    val (user, setUser) = useState(UserInfo(name = ""))

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
                props.namesToSkip.joinToString(DATABASE_DELIMITER)
                    .let { names -> if (names.isNotBlank()) "&namesToSkip=$names" else "" }
                    .let { names -> "$apiUrl/users/by-prefix/?prefix=$prefix$names" }
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
     * [Set] of names to ignore when showing results
     */
    var namesToSkip: Set<String>
}
