@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.core.result.Crash
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.domain.SandboxFileInfo
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.codeeditor.sandboxCodeEditorComponent
import com.saveourtool.save.frontend.components.basic.fileuploader.fileUploaderForSandbox
import com.saveourtool.save.frontend.components.basic.sdkSelection
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.largeTransparentModalStyle
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.externals.fontawesome.faArrowLeft
import com.saveourtool.save.frontend.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*

import csstype.AlignItems
import csstype.ClassName
import csstype.Color
import csstype.Display
import io.ktor.http.*
import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.aria.AriaRole
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.p

import kotlinx.browser.window
import kotlinx.coroutines.launch

val sandboxApiUrl = "${window.location.origin}/sandbox/api"

/**
 * [State] for [SandboxView]
 */
external interface SandboxViewState : State, HasSelectedMenu<ContestMenuBar> {
    /**
     * Selected files
     */
    var files: List<SandboxFileInfo>

    /**
     * Result of save-cli execution
     */
    var debugInfo: TestResultDebugInfo?

    /**
     * Selected SDK
     */
    var selectedSdk: Sdk

    /**
     * Flag that displays if modal is open
     */
    var isModalOpen: Boolean
}

/**
 * A view for testing config files
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class SandboxView : AbstractView<Props, SandboxViewState>(true) {
    private val closeModal = {
        setState {
            isModalOpen = false
        }
    }
    init {
        state.debugInfo = null
        state.files = listOf()
        state.selectedSdk = Sdk.Default
        state.isModalOpen = false
    }

    override fun ChildrenBuilder.render() {
        renderHeader()

        state.debugInfo?.let { debugInfo ->
            displayModal(
                isOpen = state.isModalOpen,
                title = "Additional debug info",
                classes = "modal-lg",
                bodyBuilder = { displayTestResultDebugInfo(debugInfo) },
                modalStyle = largeTransparentModalStyle,
                onCloseButtonPressed = closeModal,
            ) {
                buttonBuilder("Ok") { closeModal() }
            }
        }

        div {
            className = ClassName("d-flex justify-content-center")
            div {
                className = ClassName(" flex-wrap col-10")

                renderDebugInfo()
                renderCodeEditor()

                div {
                    className = ClassName("row mt-3 mb-3")
                    div {
                        className = ClassName("col-4")
                        div {
                            className = ClassName("card")
                            div {
                                className = ClassName("row")
                                renderToolUpload()
                                renderUploadHint()
                            }
                        }
                    }
                    div {
                        className = ClassName("col-8")
                        // ======== sdk selection =========
                        sdkSelection {
                            title = ""
                            selectedSdk = state.selectedSdk
                            onSdkChange = { newSdk ->
                                setState {
                                    selectedSdk = newSdk
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ChildrenBuilder.renderHeader() {
        h2 {
            className = ClassName("text-center mt-3")
            style = jso {
                color = Color("#FFFFFF")
            }
            +"Sandbox"
        }

        h4 {
            className = ClassName("text-center")
            +"try your SAVE configuration online"
        }
    }

    private fun ChildrenBuilder.renderCodeEditor() {
        div {
            className = ClassName("")
            sandboxCodeEditorComponent {
                editorTitle = ""
                doRunExecution = ::runExecution
                doResultReload = ::resultReload
            }
        }
    }

    private fun ChildrenBuilder.renderUploadHint() {
        div {
            className = ClassName("col-6")
            style = jso {
                display = Display.flex
                alignItems = AlignItems.flexEnd
            }

            p {
                className = ClassName("text-info mt-1")
                fontAwesomeIcon(icon = faArrowLeft)
                +" upload your tested tool and all other needed files"
            }
        }
    }

    private fun ChildrenBuilder.renderDebugInfo() {
        state.debugInfo?.let { debugInfo ->
            div {
                val alertStyle = when (debugInfo.testStatus) {
                    is Pass -> "success"
                    is Fail -> "danger"
                    is Ignored -> "secondary"
                    is Crash -> "danger"
                    else -> "info"
                }
                div {
                    className = ClassName("alert alert-$alertStyle alert-dismissible fade show")
                    role = "alert".unsafeCast<AriaRole>()
                    div {
                        displayTestResultDebugInfoStatus(debugInfo)
                        buttonBuilder("See more details...", "link", classes = "p-0") {
                            setState {
                                isModalOpen = true
                            }
                        }
                    }
                    button {
                        type = ButtonType.button
                        className = ClassName("align-self-center close")
                        asDynamic()["data-dismiss"] = "alert"
                        ariaLabel = "Close"
                        fontAwesomeIcon(icon = faTimesCircle)
                        onClick = {
                            setState {
                                this.debugInfo = null
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ChildrenBuilder.renderToolUpload() {
        div {
            className = ClassName("col-6")
            fileUploaderForSandbox(
                state.files,
                { fileToAdd ->
                    setState {
                        files = files + fileToAdd
                    }
                }
            ) { fileToDelete ->
                setState {
                    files = files - fileToDelete
                }
            }
        }
    }

    private fun resultReload() {
        scope.launch {
            val response = get(
                "$sandboxApiUrl/get-debug-info",
                Headers().apply {
                    set("Accept", "application/octet-stream")
                },
                loadingHandler = ::classLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )

            if (response.ok) {
                val resultDebugInfo: TestResultDebugInfo = response.decodeFromJsonString()
                setState {
                    debugInfo = resultDebugInfo
                }
            } else {
                window.alert("There is no debug info yet. Try to run execution and wait until it is finished.")
            }
        }
    }

    private fun runExecution(successMessage: String) {
        scope.launch {
            val response = post(
                url = "$sandboxApiUrl/run-execution?sdk=${state.selectedSdk}",
                headers = jsonHeaders,
                body = undefined,
                loadingHandler = ::classLoadingHandler,
                responseHandler = ::classComponentResponseHandlerWithValidation,
            )
            if (response.ok) {
                window.alert(successMessage)
            } else if (response.isConflict()) {
                window.alert("There is already a running execution")
            }
        }
    }

    companion object :
        RStatics<Props, SandboxViewState, SandboxView, Context<RequestStatusContext>>(SandboxView::class) {
        init {
            ContestView.contextType = requestStatusContext
        }
    }
}
