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
import com.saveourtool.save.frontend.components.basic.codeeditor.FileType
import com.saveourtool.save.frontend.components.basic.codeeditor.codeEditorComponent
import com.saveourtool.save.frontend.components.basic.fileUploaderForSandbox
import com.saveourtool.save.frontend.components.basic.sdkSelection
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.largeTransparentModalStyle
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.info.UserInfo

import csstype.ClassName
import org.w3c.fetch.Headers
import react.*
import react.dom.aria.AriaRole
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

val sandboxApiUrl = "${window.location.origin}/sandbox/api"

/**
 * [Props] of [SandboxView]
 */
external interface SandboxViewProps : Props {
    /**
     * [UserInfo] of a current user
     */
    var currentUserInfo: UserInfo?
}

/**
 * [State] for [SandboxView]
 */
external interface SandboxViewState : State, HasSelectedMenu<ContestMenuBar> {
    /**
     * Code from text editor
     */
    var codeText: String

    /**
     * Config from text editor
     */
    var configText: String

    /**
     * setup.sh from text editor
     */
    var setupShText: String

    /**
     * Selected files
     */
    var files: List<SandboxFileInfo>

    /**
     * Result of save-cli execution
     */
    var debugInfo: TestResultDebugInfo?

    /**
     * Currently selected FileType - config, test or setup.sh
     */
    var selectedFile: FileType?

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
class SandboxView : AbstractView<SandboxViewProps, SandboxViewState>(true) {
    private val closeModal = {
        setState {
            isModalOpen = false
        }
    }
    init {
        state.codeText = ""
        state.configText = ""
        state.setupShText = ""
        state.debugInfo = null
        state.files = listOf()
        state.selectedFile = null
        state.selectedSdk = Sdk.Default
        state.isModalOpen = false
    }

    override fun ChildrenBuilder.render() {
        h3 {
            className = ClassName("text-center")
            +"Sandbox"
        }

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

                renderToolUpload()

                // ======== sdk selection =========
                sdkSelection {
                    title = "Select the SDK:"
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

    private fun ChildrenBuilder.renderCodeEditor() {
        div {
            className = ClassName("")
            codeEditorComponent {
                editorTitle = "Code editor"
                selectedFile = state.selectedFile
                onSelectedFileUpdate = {
                    setState { selectedFile = it }
                }
                editorText = when (state.selectedFile) {
                    FileType.CODE -> state.codeText
                    FileType.SAVE_TOML -> state.configText
                    FileType.SETUP_SH -> state.setupShText
                    else -> "Press on any of the buttons above to start editing"
                }
                onEditorTextUpdate = {
                    setState {
                        when (state.selectedFile) {
                            FileType.CODE -> codeText = it
                            FileType.SAVE_TOML -> configText = it
                            FileType.SETUP_SH -> setupShText = it
                            else -> { }
                        }
                    }
                }
                doUploadChanges = ::uploadChanges
                doReloadChanges = ::reloadChanges
                doRunExecution = ::runExecution
                doResultReload = ::resultReload
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
                        a {
                            role = "button".unsafeCast<AriaRole>()
                            p {
                                className = ClassName("font-italic mb-0")
                                strong {
                                    +"See more details..."
                                }
                            }
                            onClick = {
                                setState {
                                    isModalOpen = true
                                }
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
            className = ClassName("m-3")
            div {
                className = ClassName("d-flex justify-content-center")
                fileUploaderForSandbox(
                    props.currentUserInfo?.name,
                    state.files
                ) { selectedFiles ->
                    setState {
                        files = selectedFiles
                    }
                }
            }
        }
    }

    private fun uploadChanges() {
        scope.launch {
            postContentAsText("test", "test", state.codeText)
            postContentAsText("test", "save.toml", state.configText)
            postContentAsText("file", "setup.sh", state.setupShText)
        }
    }

    private fun reloadChanges() {
        scope.launch {
            val newCodeText = getContentAsText("test", "test", codeExample)
            val newConfigText = getContentAsText("test", "save.toml", configExample)
            val newSetupShText = getContentAsText("file", "setup.sh", setupShExample)

            setState {
                codeText = newCodeText
                configText = newConfigText
                setupShText = newSetupShText
            }
        }
    }

    private suspend fun postContentAsText(
        urlPart: String,
        fileName: String,
        text: String,
    ) {
        post(
            url = "$sandboxApiUrl/upload-$urlPart-as-text?userName=${props.currentUserInfo?.name}&fileName=$fileName",
            headers = jsonHeaders,
            body = text,
            loadingHandler = ::noopLoadingHandler,
        )
    }

    private suspend fun getContentAsText(
        urlPart: String,
        fileName: String,
        defaultValue: String,
    ): String = props.currentUserInfo?.name?.let { userName ->
        val response = get(
            url = "$sandboxApiUrl/download-$urlPart-as-text?userName=$userName&fileName=$fileName",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
        if (response.ok) {
            response.text().await()
        } else {
            postContentAsText(urlPart, fileName, defaultValue)
            defaultValue
        }
    } ?: "Unknown user"

    private fun resultReload() {
        scope.launch {
            val resultDebugInfo: TestResultDebugInfo = get(
                "$sandboxApiUrl/get-debug-info?userName=${props.currentUserInfo?.name}",
                Headers().apply {
                    set("Accept", "application/octet-stream")
                },
                loadingHandler = ::classLoadingHandler,
            )
                .decodeFromJsonString()
            setState {
                debugInfo = resultDebugInfo
            }
        }
    }

    private fun runExecution() {
        scope.launch {
            post(
                url = "$sandboxApiUrl/run-execution?userName=${props.currentUserInfo?.name}&sdk=${state.selectedSdk}",
                headers = jsonHeaders,
                body = undefined,
                loadingHandler = ::noopLoadingHandler,
            )
        }
    }

    companion object : RStatics<SandboxViewProps, SandboxViewState, SandboxView, Context<RequestStatusContext>>(SandboxView::class) {
        private val configExample = """
            |[general]
            |tags = ["demo"]
            |description = "saveourtool online demo"
            |suiteName = "Test"
            |execCmd="RUN_COMMAND"
            |language = "Kotlin"
            |
            |[warn]
            |execFlags = "--build-upon-default-config -i"
            |actualWarningsPattern = "\\w+ - (\\d+)/(\\d+) - (.*)${'$'}" # (default value)
            |testNameRegex = ".*Test.*" # (default value)
            |patternForRegexInWarning = ["{{", "}}"]
            |# Extra flags will be extracted from a line that mathces this regex if it's present in a file
            |runConfigPattern = "# RUN: (.+)"
        """.trimMargin()
        private val codeExample = """
            |package com.example
            |
            |data class BestLanguage(val name = "Kotlin")
            |
            |fun main {
            |    val bestLanguage = BestLanguage()
            |    println("saveourtool loves ${ '$' }{bestLanguage.name}")
            |}
        """.trimMargin()
        private val setupShExample = """
            |# Here you can add some additional commands required to run your tool e.g.
            |# python -m pip install pylint
        """.trimMargin()
        init {
            ContestView.contextType = requestStatusContext
        }
    }
}
