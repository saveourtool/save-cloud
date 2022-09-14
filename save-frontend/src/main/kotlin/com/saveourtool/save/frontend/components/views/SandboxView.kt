@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.sandbox.sandboxCodeEditorComponent
import com.saveourtool.save.frontend.components.basic.sandbox.sandboxConfigEditorComponent
import com.saveourtool.save.frontend.components.basic.selectFormRequired
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.externals.fontawesome.faUpload
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.externals.reactace.AceThemes
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

import kotlinx.coroutines.launch

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
     * Currently selected theme
     */
    var selectedTheme: AceThemes

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
}

/**
 * A view for testing config files
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class SandboxView : AbstractView<SandboxViewProps, SandboxViewState>(false) {
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    private val themeSelector = selectFormRequired<AceThemes>()
    init {
        state.codeText = codeExample
        state.configText = configExample
        state.debugInfo = null
        state.selectedTheme = AceThemes.preferredTheme
        state.files = mutableListOf()
        state.suiteByteSize = 0
        state.isUploading = false
        state.bytesReceived = 0
    }

    override fun ChildrenBuilder.render() {
        h3 {
            className = ClassName("text-center")
            +"Sandbox"
        }
        renderDebugInfo()

        renderThemeSelector()

        div {
            className = ClassName("d-flex justify-content-around")
            div {
                className = ClassName("column")
                renderCodeEditor()
            }
            div {
                className = ClassName("column")
                renderConfigEditor()
            }
        }
        renderToolUpload()
    }

    private fun ChildrenBuilder.renderInstallShEditor() {
        div {
            className = ClassName("text-center")
            +"InstallShEditor"
        }
    }

    private fun ChildrenBuilder.renderCodeEditor() {
        sandboxCodeEditorComponent {
            codeText = state.codeText
            selectedTheme = state.selectedTheme
            onCodeTextUpdate = {
                setState {
                    codeText = it
                }
            }
        }
    }

    private fun ChildrenBuilder.renderConfigEditor() {
        sandboxConfigEditorComponent {
            configText = state.configText
            selectedTheme = state.selectedTheme
            onConfigTextUpdate = {
                setState {
                    configText = it
                }
            }
        }
    }

    private fun ChildrenBuilder.renderThemeSelector() {
        div {
            className = ClassName("d-flex justify-content-center")
            themeSelector {
                formType = InputTypes.ACE_THEME_SELECTOR
                validInput = null
                classes = "col-2"
                selectClasses = "custom-select custom-select-sm"
                getData = {
                    AceThemes.values().toList()
                }
                selectedValue = state.selectedTheme.themeName
                dataToString = { it.themeName }
                errorMessage = null
                notFoundErrorMessage = null
                onChangeFun = { theme ->
                    theme?.let {
                        setState {
                            selectedTheme = theme
                        }
                    }
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
        init {
            ContestView.contextType = requestStatusContext
        }
    }
}
