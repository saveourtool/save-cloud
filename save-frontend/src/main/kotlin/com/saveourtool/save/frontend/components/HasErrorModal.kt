@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components

import com.saveourtool.save.frontend.components.views.FallbackView
import com.saveourtool.save.frontend.externals.modal.modal
import com.saveourtool.save.frontend.topBarComponent

import csstype.ClassName
import org.w3c.fetch.Response
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.span

import kotlinx.browser.window

/**
 * Context to store data about current request such as errors and isLoading flag.
 */
@Suppress("TYPE_ALIAS")
val requestStatusContext: Context<RequestStatusContext> = createContext()

/**
 * Component that displays generic warning about unsuccessful request based on info in [requestStatusContext].
 * Also renders its `children`.
 */
@Suppress("TOO_MANY_LINES_IN_LAMBDA", "MAGIC_NUMBER")
val requestModalHandler: FC<PropsWithChildren> = FC { props ->
    val (response, setResponse) = useState<Response?>(null)
    val (loadingCounter, setLoadingCounter) = useState(0)
    val statusContext = RequestStatusContext(setResponse, setLoadingCounter)
    val (modalState, setModalState) = useState(ErrorModalState(
        isErrorModalOpen = false,
        errorMessage = "",
        errorLabel = "",
        status = null,
    ))
    val (loadingState, setLoadingState) = useState(LoadingModalState(
        false,
    ))

    useEffect(response) {
        val newModalState = when (response?.status) {
            401.toShort() -> ErrorModalState(
                isErrorModalOpen = true,
                errorMessage = "You are not logged in",
                errorLabel = "Unauthenticated",
                confirmationText = "Proceed to login page",
                status = response.status,
            )
            404.toShort() -> ErrorModalState(
                isErrorModalOpen = false,
                errorMessage = "${response.status} ${response.statusText}",
                errorLabel = response.status.toString(),
                status = response.status,
            )
            else -> ErrorModalState(
                isErrorModalOpen = response != null,
                errorMessage = "${response?.status} ${response?.statusText}",
                errorLabel = response?.status.toString(),
                status = response?.status,
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

    useEffect(loadingCounter) {
        if (loadingCounter != 0) {
            setLoadingState(LoadingModalState(true))
        } else {
            setLoadingState(LoadingModalState(false))
        }
    }

    modal { modalProps ->
        modalProps.isOpen = loadingState.isLoadingModalOpen
        div {
            className = ClassName("d-flex justify-content-center mt-4")
            div {
                className = ClassName("spinner-border text-primary spinner-border-lg")
                span {
                    className = ClassName("sr-only")
                    +"Loading..."
                }
            }
        }
    }

    val contextPayload = useMemo(
        arrayOf(statusContext)
    ) { statusContext }

    val reactNode = if (modalState.status == 404.toShort()) {
        div.create {
            className = ClassName("d-flex flex-column")
            id = "content-wrapper"
            topBarComponent()
            div {
                className = ClassName("container-fluid")
                FallbackView::class.react {
                    bigText = "404"
                    smallText = "Page not found"
                    withRouterLink = false
                }
            }
            Footer::class.react()
        }
    } else {
        props.children
    }

    requestStatusContext.Provider {
        value = contextPayload
        +reactNode
    }
}

/**
 * @property setResponse [StateSetter] for response error handler
 * @property setLoadingCounter [StateSetter] for active request counter
 */
data class RequestStatusContext(
    val setResponse: StateSetter<Response?>,
    val setLoadingCounter: StateSetter<Int>,
)

/**
 * @property isErrorModalOpen
 * @property errorMessage
 * @property errorLabel
 * @property confirmationText text that will be displayed on modal dismiss button
 * @property status
 */
data class ErrorModalState(
    val isErrorModalOpen: Boolean?,
    val errorMessage: String,
    val errorLabel: String,
    val confirmationText: String = "Close",
    val status: Short?,
)

/**
 * @property isLoadingModalOpen
 */
data class LoadingModalState(
    val isLoadingModalOpen: Boolean?,
)
