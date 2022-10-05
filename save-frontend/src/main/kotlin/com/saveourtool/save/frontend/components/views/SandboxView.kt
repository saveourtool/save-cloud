@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.codeeditor.FileType
import com.saveourtool.save.frontend.components.basic.codeeditor.codeEditorComponent
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.externals.fontawesome.faUpload
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.info.UserInfo

import csstype.ClassName
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.xhr.FormData
import react.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
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
    var files: MutableList<FileInfo>

    /**
     * Flag that indicates if any file is uploading
     */
    var isUploading: Boolean

    /**
     * Bytes to send
     */
    var suiteByteSize: Long

    /**
     * Bytes sent to server
     */
    var bytesReceived: Long

    /**
     * Result of save-cli execution
     */
    var debugInfo: String?

    /**
     * Currently selected FileType - config, test or setup.sh
     */
    var selectedFile: FileType?
}

/**
 * A view for testing config files
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class SandboxView : AbstractView<SandboxViewProps, SandboxViewState>(false) {
    init {
        state.codeText = ""
        state.configText = ""
        state.setupShText = ""
        state.debugInfo = null
        state.files = mutableListOf()
        state.suiteByteSize = 0
        state.isUploading = false
        state.bytesReceived = 0
        state.selectedFile = null
    }

    override fun ChildrenBuilder.render() {
        h3 {
            className = ClassName("text-center")
            +"Sandbox"
        }
        renderCodeEditor()

        renderDebugInfo()

        renderToolUpload()
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
                doUploadChanges = {
                    uploadTests()
                }
                doReloadChanges = {
                    loadTests()
                }
            }
        }
    }

    // private fun ChildrenBuilder.renderFileSelector() {
    // div {
    // className = ClassName("d-flex justify-content-end")
    // button {
    // className = ClassName("btn btn-info-outline")
    // asDynamic()["data-toggle"] = "collapse"
    // asDynamic()["data-target"] = "#codeEditorFileSelector"
    // asDynamic()["data-controls"] = "codeEditorFileSelector"
    // ariaExpanded = false
    // onClick = {
    // setState {
    // isFileSelectorCollapsed = !isFileSelectorCollapsed
    // }
    // }
    // +">>>"
    // }
    // displayCodeEditorFileSelector(
    // state.selectedFileType,
    // ) {
    // setState {
    // selectedFileType = it
    // }
    // }
    // }
    // }

    private fun ChildrenBuilder.renderDebugInfo() {
        state.debugInfo?.let { debugInfo ->
            div {
                h6 {
                    className = ClassName("text-center")
                    +"DebugInfo"
                }
                +debugInfo
            }
        }
    }

    private fun ChildrenBuilder.renderToolUpload() {
        div {
            className = ClassName("m-3")
            div {
                className = ClassName("d-flex justify-content-center")
                label {
                    className = ClassName("btn btn-outline-secondary")
                    input {
                        type = InputType.file
                        multiple = true
                        hidden = true
                        onChange = {
                            onFileInput(it.target)
                        }
                    }
                    fontAwesomeIcon(icon = faUpload)
                    asDynamic()["data-toggle"] = "tooltip"
                    asDynamic()["data-placement"] = "top"
                    title = "Regular files/Executable files/ZIP Archives"
                    strong { +"Upload tool" }
                }
            }
        }
    }

    private fun onFileInput(element: HTMLInputElement) {
        scope.launch {
            setState {
                isUploading = true
                element.files!!.asList().forEach { file ->
                    suiteByteSize += file.size.toLong()
                }
            }

            element.files!!.asList().forEach { file ->
                val response: FileInfo = post(
                    "",
                    Headers(),
                    FormData().apply {
                        append("file", file)
                    },
                    loadingHandler = ::noopLoadingHandler,
                )
                    .decodeFromJsonString()

                setState {
                    // add only to selected files so that this entry isn't duplicated
                    files.add(response)
                    bytesReceived += response.sizeBytes
                }
            }
            setState {
                isUploading = false
            }
        }
    }

    private fun uploadTests() {
        scope.launch {
            setState {
                isUploading = true
            }

            postTestAsText("test", "test", state.codeText)
            postTestAsText("test-resource", "save.toml", state.configText)
            postTestAsText("test-resource", "setup.sh", state.setupShText)

            setState {
                isUploading = false
            }
        }
    }

    private fun loadTests() {
        scope.launch {
            val newCodeText = getTestAsText("test", "test", codeExample)
            val newConfigText = getTestAsText("test-resource", "save.toml", configExample)
            val newSetupShText = getTestAsText("test-resource", "setup.sh", setupShExample)

            setState {
                codeText = newCodeText
                configText = newConfigText
                setupShText = newSetupShText
            }
        }
    }

    private suspend fun postTestAsText(
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

    private suspend fun getTestAsText(
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
            postTestAsText(urlPart, fileName, defaultValue)
            defaultValue
        }
    } ?: "Unknown user"

    companion object : RStatics<SandboxViewProps, SandboxViewState, SandboxView, Context<RequestStatusContext>>(SandboxView::class) {
        private val configExample = """
            |[general]
            |tags = ["demo"]
            |description = "saveourtool online demo"
            |suiteName = Test
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
            |python3.10 -m pip install pylint
        """.trimMargin()
        init {
            ContestView.contextType = requestStatusContext
        }
    }
}
