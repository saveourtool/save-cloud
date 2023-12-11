@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.core.result.Crash
import com.saveourtool.save.core.result.Fail
import com.saveourtool.save.core.result.Ignored
import com.saveourtool.save.core.result.Pass
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.frontend.common.components.modal.displayModal
import com.saveourtool.save.frontend.common.components.modal.largeTransparentModalStyle
import com.saveourtool.save.frontend.common.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.common.utils.*
import com.saveourtool.save.frontend.common.utils.get
import com.saveourtool.save.frontend.components.basic.codeeditor.sandboxCodeEditorComponent
import com.saveourtool.save.frontend.components.basic.fileuploader.sandboxFileUploader
import com.saveourtool.save.frontend.components.basic.sdkSelection

import io.ktor.http.*
import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.aria.AriaRole
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import web.cssom.ClassName
import web.cssom.Color
import web.html.ButtonType

import kotlinx.browser.window

val sandboxApiUrl = "${window.location.origin}/api/sandbox"

/**
 * A view for testing config files
 */
val sandboxView: FC<Props> = FC {
    useBackground(Style.SAVE_DARK)
    val (debugInfo, setDebugInfo) = useState<TestResultDebugInfo?>(null)
    val (selectedSdk, setSelectedSdk) = useState<Sdk>(Sdk.Default)
    val (isModalOpen, setIsModalOpen) = useState(false)

    val resultReload = useDeferredRequest {
        val response = get(
            "$sandboxApiUrl/get-debug-info",
            Headers().withAcceptOctetStream(),
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )

        if (response.ok) {
            val resultDebugInfo: TestResultDebugInfo = response.decodeFromJsonString()
            setDebugInfo(resultDebugInfo)
        } else {
            window.alert("There is no debug info yet. Try to run execution and wait until it is finished.")
        }
    }

    val runExecution = useDeferredRequest {
        val response = post(
            url = "$sandboxApiUrl/run-execution?sdk=$selectedSdk",
            headers = jsonHeaders,
            body = undefined,
            loadingHandler = ::loadingHandler,
            responseHandler = ::responseHandlerWithValidation,
        )
        if (response.ok) {
            window.alert("Successfully saved and started execution")
        } else if (response.isConflict()) {
            window.alert("There is already a running execution")
        }
    }

    h2 {
        className = ClassName("text-center mt-3")
        style = jso { color = Color("#FFFFFF") }
        +"Sandbox"
    }

    h4 {
        className = ClassName("text-center")
        +"try your SAVE configuration online"
    }
    debugInfo?.let {
        displayModal(
            isOpen = isModalOpen,
            title = "Additional debug info",
            classes = "modal-lg",
            bodyBuilder = { displayTestResultDebugInfo(debugInfo) },
            modalStyle = largeTransparentModalStyle,
            onCloseButtonPressed = { setIsModalOpen(false) },
        ) {
            buttonBuilder("Ok") { setIsModalOpen(false) }
        }
    }
    div {
        className = ClassName("d-flex justify-content-center")
        div {
            className = ClassName(" flex-wrap col-10")
            debugInfo?.let { debugInfo ->
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
                                setIsModalOpen(true)
                            }
                        }
                        button {
                            type = ButtonType.button
                            className = ClassName("align-self-center close")
                            asDynamic()["data-dismiss"] = "alert"
                            ariaLabel = "Close"
                            fontAwesomeIcon(icon = faTimesCircle)
                            onClick = { setDebugInfo(null) }
                        }
                    }
                }
            }
            div {
                className = ClassName("")
                sandboxCodeEditorComponent {
                    editorTitle = ""
                    doRunExecution = runExecution
                    doResultReload = resultReload
                }
            }
            div {
                className = ClassName("row mt-3 mb-3")
                div {
                    className = ClassName("col-4")
                    sandboxFileUploader {
                        getUrlForAvailableFilesFetch = { "$sandboxApiUrl/list-file" }
                        getUrlForFileUpload = { "$sandboxApiUrl/upload-file" }
                        getUrlForFileDownload = { fileInfo ->
                            "$sandboxApiUrl/download-file?fileName=${fileInfo.name.escapeIfNeeded()}"
                        }
                        getUrlForFileDeletion = { fileInfo ->
                            "$sandboxApiUrl/delete-file?fileName=${fileInfo.name.escapeIfNeeded()}"
                        }
                    }
                }
                div {
                    className = ClassName("col-8")
                    // ======== sdk selection =========
                    sdkSelection {
                        title = ""
                        this.selectedSdk = selectedSdk
                        onSdkChange = { newSdk -> setSelectedSdk(newSdk) }
                    }
                }
            }
        }
    }
}
