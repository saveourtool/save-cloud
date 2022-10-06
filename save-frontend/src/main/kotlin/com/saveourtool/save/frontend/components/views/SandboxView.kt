@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.SandboxFileInfo
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.codeeditor.FileType
import com.saveourtool.save.frontend.components.basic.codeeditor.codeEditorComponent
import com.saveourtool.save.frontend.components.basic.fileUploaderForSandbox
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.info.UserInfo

import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.h6

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
class SandboxView : AbstractView<SandboxViewProps, SandboxViewState>(true) {
    init {
        state.codeText = ""
        state.configText = ""
        state.setupShText = ""
        state.debugInfo = null
        state.files = listOf()
        state.selectedFile = null
    }

    override fun ChildrenBuilder.render() {
        h3 {
            className = ClassName("text-center")
            +"Sandbox"
        }
        div {
            className = ClassName("d-flex justify-content-center")
            div {
                className = ClassName(" flex-wrap col-10")
                renderCodeEditor()

                renderDebugInfo()

                renderToolUpload()
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
                doUploadChanges = {
                    uploadChanges()
                }
                doReloadChanges = {
                    reloadChanges()
                }
            }
        }
    }

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
