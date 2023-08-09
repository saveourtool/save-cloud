/**
 * rendering for Delete User management card
 */

package com.saveourtool.save.frontend.components.views.usersettings.right

import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.views.usersettings.SettingsProps
import com.saveourtool.save.frontend.utils.*
import js.core.jso
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.fetch.Headers
import react.FC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h3
import web.cssom.ClassName
import web.cssom.rem

val deleteSettingsCard: FC<SettingsProps> = FC { props ->
    val deleteUserWindowOpenness = useWindowOpenness()

    displayModal(
        deleteUserWindowOpenness.isOpen(),
        "Deletion of user profile",
        "Are you sure you want to permanently delete your profile? You will never be able to restore it again.",
        mediumTransparentModalStyle,
        deleteUserWindowOpenness.closeWindowAction(),
    ) {
        buttonBuilder("Yes", isActive = props.userInfo != null) {
            deleteUser(props.userInfo?.name!!)
            deleteUserWindowOpenness.closeWindow()
        }
        buttonBuilder("Cancel", "secondary") {
            deleteUserWindowOpenness.closeWindow()
        }
    }

    div {
        className = ClassName("row justify-content-center mt-5")
        ReactHTML.img {
            src = "/img/sad_cat.png"
            @Suppress("MAGIC_NUMBER")
            style = jso {
                width = 14.rem
            }
        }
    }

    div {
        className = ClassName("row align-items-center justify-content-center mt-4")
        h2 {
            className = ClassName("mt-2 mr-2 text-gray-800")
            +"Want to leave us?"
        }
    }

    div {
        className = ClassName("row align-items-center justify-content-center")
        div {
            className = ClassName("col-4 text-center")
            buttonBuilder("Delete your profile", style = "danger") {
                deleteUserWindowOpenness.openWindow()
            }
        }
    }
}

@Suppress("MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
fun deleteUser(userName: String) {
    useRequest {
        val response = get(
            url = "$apiUrl/users/delete/$userName",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
        if (response.ok) {
            val replyToLogout = post(
                "${window.location.origin}/logout",
                Headers(),
                "ping",
                loadingHandler = ::loadingHandler,
            )
            if (replyToLogout.ok) {
                window.location.href = "${window.location.origin}/#"
                window.location.reload()
            }
        }
    }
}
