@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components

import org.cqfn.save.frontend.externals.modal.modal
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

/**
 * Context to store data about current request error.
 */
val errorStatusContext: Context<StateSetter<Response?>> = createContext()

/**
 * Component that displays generic warning about unsuccessful request based on info in [errorStatusContext].
 * Also renders its `children`.
 */
val errorModalHandler: FC<PropsWithChildren> = FC { props ->
    val (response, setResponse) = useState<Response?>(null)
    val (modalState, setModalState) = useState(ErrorModalState(
        isErrorModalOpen = false,
        errorMessage = "",
        errorLabel = "",
    ))

    useEffect(response) {
        val newModalState = ErrorModalState(
            isErrorModalOpen = response != null,
            errorMessage = "${response?.status} ${response?.statusText}",
            errorLabel = response?.status.toString(),
        )
        setModalState(newModalState)
    }

    modal {
        it.isOpen = modalState.isErrorModalOpen
        it.contentLabel = modalState.errorLabel
        div {
            className = "row align-items-center justify-content-center"
            h2 {
                className = "h6 text-gray-800"
                +modalState.errorMessage
            }
        }
        div {
            className = "d-sm-flex align-items-center justify-content-center mt-4"
            button {
                className = "btn btn-primary"
                type = ButtonType.button
                onClick = {
                    setResponse(null)
                    setModalState(modalState.copy(isErrorModalOpen = false))
                }
                +"Close"
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
 */
data class ErrorModalState(
    val isErrorModalOpen: Boolean?,
    val errorMessage: String,
    val errorLabel: String,
)
