@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components

import org.cqfn.save.frontend.externals.modal.modal

import csstype.ClassName
import org.w3c.fetch.Response
import react.Context
import react.FC
import react.PropsWithChildren
import react.StateSetter
import react.createContext
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.useEffect
import react.useMemo
import react.useState

import kotlinx.browser.window

/**
 * Context to store data about current request error.
 */
@Suppress("TYPE_ALIAS")
val errorStatusContext: Context<StateSetter<Response?>> = createContext()

/**
 * Component that displays generic warning about unsuccessful request based on info in [errorStatusContext].
 * Also renders its `children`.
 */
@Suppress("TOO_MANY_LINES_IN_LAMBDA", "MAGIC_NUMBER")
val errorModalHandler: FC<PropsWithChildren> = FC { props ->
    val (response, setResponse) = useState<Response?>(null)
    val (modalState, setModalState) = useState(ErrorModalState(
        isErrorModalOpen = false,
        errorMessage = "",
        errorLabel = "",
    ))

    useEffect(response) {
        val newModalState = if (response?.status == 401.toShort()) {
            ErrorModalState(
                isErrorModalOpen = true,
                errorMessage = "You are not logged in",
                errorLabel = "Unauthenticated",
                confirmationText = "Proceed to login page",
            )
        } else {
            ErrorModalState(
                isErrorModalOpen = response != null,
                errorMessage = "${response?.status} ${response?.statusText}",
                errorLabel = response?.status.toString(),
            )
        }
        setModalState(newModalState)
    }

    modal { modalProps ->
        modalProps.isOpen = modalState.isErrorModalOpen
        modalProps.contentLabel = modalState.errorLabel
        div {
            className = ClassName("row align-items-center justify-content-center")
            h2 {
                className = ClassName("h6 text-gray-800")
                +modalState.errorMessage
            }
        }
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")
            button {
                className = ClassName("btn btn-primary")
                type = ButtonType.button
                onClick = {
                    if (response?.status == 401.toShort()) {
                        // if 401 - change current URL to the main page (with login screen)
                        window.location.href = "${window.location.origin}/#"
                        window.location.reload()
                    }
                    setResponse(null)
                    setModalState(modalState.copy(isErrorModalOpen = false))
                }
                +modalState.confirmationText
            }
        }
    }

    val contextPayload = useMemo(
        arrayOf(setResponse)
    ) { setResponse }

    errorStatusContext.Provider {
        value = contextPayload
        +props.children
    }
}

/**
 * @property isErrorModalOpen
 * @property errorMessage
 * @property errorLabel
 * @property confirmationText text that will be displayed on modal dismiss button
 */
data class ErrorModalState(
    val isErrorModalOpen: Boolean?,
    val errorMessage: String,
    val errorLabel: String,
    val confirmationText: String = "Close",
)
