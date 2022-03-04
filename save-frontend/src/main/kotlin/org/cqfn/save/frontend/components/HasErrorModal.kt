package org.cqfn.save.frontend.components

import org.cqfn.save.frontend.externals.modal.modal
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

data class ErrorModalState(
    val isErrorModalOpen: Boolean?,
    val errorMessage: String,
    val errorLabel: String,
)

val errorStatusContext = createContext<StateSetter<Int?>>()

val errorModalHandler = FC<PropsWithChildren> { props ->
    val (errorCode, setErrorCode) = useState<Int?>(null)
    val (modalState, setModalState) = useState(ErrorModalState(
        isErrorModalOpen = false,
        errorMessage = "",
        errorLabel = "",
    ))

    useEffect(arrayOf<dynamic>(errorCode)) {
        val newModalState = ErrorModalState(
            isErrorModalOpen = errorCode != null,
            errorMessage = "$errorCode",
            errorLabel = errorCode.toString(),
        )
        if (newModalState != modalState) {
            setModalState(
                newModalState
            )
        }
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
                    setErrorCode(null)
                    setModalState(modalState.copy(isErrorModalOpen = false))
                }
                +"Close"
            }
        }
    }

    val contextPayload = useMemo(
        arrayOf(setErrorCode)
    ) { setErrorCode }

    errorStatusContext.Provider {
        value = contextPayload
        +props.children
    }
}
