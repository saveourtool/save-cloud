/**
 * Function component for different demos
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.demo.diktat.*
import com.saveourtool.save.frontend.components.basic.codeeditor.codeEditorComponent
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.Languages

import csstype.ClassName
import js.core.asList
import react.*
import react.dom.aria.AriaRole
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import web.file.FileReader

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val diktatDemoComponent = diktatDemoComponent()

private val diktatDemoDefaultCode = """
    |package com.test
    |
    |fun main() {
    |    val SAVEOUR = "tools"
    |}
""".trimMargin()

/**
 * DemoComponent [Props]
 */
external interface DiktatDemoComponentProps : Props {
    /**
     * Theme for Ace Editor
     */
    var selectedTheme: AceThemes

    /**
     * Mode for Ace Editor
     */
    var selectedMode: Languages
}

private fun ChildrenBuilder.displayAlertWithWarnings(warnings: List<String>, flushWarnings: () -> Unit) {
    div {
        val show = if (warnings.isEmpty()) {
            ""
        } else {
            "show"
        }
        val isError = warnings.singleOrNull()?.startsWith("Internal") == true
        val alertStyle = if (isError) {
            "alert-danger"
        } else {
            "alert-warning"
        }
        className = ClassName("alert $alertStyle alert-dismissible fade $show mb-0")
        role = "alert".unsafeCast<AriaRole>()
        button {
            type = ButtonType.button
            className = ClassName("close")
            ariaLabel = "Close"
            fontAwesomeIcon(faTimesCircle)
            onClick = {
                flushWarnings()
            }
        }
        if (isError) {
            h4 {
                className = ClassName("alert-heading")
                +warnings.single()
            }
        } else {
            h4 {
                className = ClassName("alert-heading")
                val warningWord = if (warnings.size == 1) {
                    "warning"
                } else {
                    "warnings"
                }
                +"Detected ${warnings.size} $warningWord:"
            }
            warnings.forEach { warning ->
                @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
                hr { }
                p {
                    className = ClassName("mb-0")
                    +warning
                }
            }
        }
    }
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "KDOC_WITHOUT_RETURN_TAG",
    "LongMethod",
    "TYPE_ALIAS"
)
private fun diktatDemoComponent() = FC<DiktatDemoComponentProps> { props ->
    val (diktatRunRequest, setDiktatRunRequest) = useState(DemoRunRequest(emptyList(), DemoAdditionalParams()))
    val (diktatResult, setDiktatResult) = useState(DiktatDemoResult(emptyList(), ""))
    val (codeLines, setCodeLines) = useState(diktatDemoDefaultCode)

    val sendRunRequest = useDeferredRequest {
        val result: DiktatDemoResult = post(
            "$demoApiUrl/diktat/run",
            jsonHeaders,
            Json.encodeToString(diktatRunRequest.copy(codeLines = codeLines.split("\n"))),
            ::loadingHandler,
            ::noopResponseHandler,
        )
            .let {
                if (it.ok) {
                    it.decodeFromJsonString()
                } else {
                    DiktatDemoResult(listOf("Internal server error."), "")
                }
            }
        setDiktatResult(result)
    }

    div {
        className = ClassName("")
        div {
            className = ClassName("row")
            div {
                className = ClassName("col-6")
                codeEditorComponent {
                    editorTitle = "Input code"
                    selectedTheme = props.selectedTheme
                    selectedMode = props.selectedMode
                    savedText = codeLines
                    draftText = codeLines
                    onDraftTextUpdate = { code ->
                        setCodeLines(code)
                    }
                    isDisabled = false
                }
            }

            div {
                className = ClassName("col-6")
                codeEditorComponent {
                    editorTitle = "Output code"
                    selectedTheme = props.selectedTheme
                    selectedMode = props.selectedMode
                    savedText = diktatResult.outputText
                    draftText = diktatResult.outputText
                    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
                    onDraftTextUpdate = { }
                    isDisabled = true
                }
            }
        }
        div {
            className = ClassName("row mt-2 mb-1 d-flex justify-content-center")
            div {
                className = ClassName("col-2 mr-1")
                selectorBuilder(
                    diktatRunRequest.params.mode.name,
                    DiktatDemoMode.values().map { it.name },
                    "custom-select"
                ) { event ->
                    setDiktatRunRequest { runRequest ->
                        runRequest.copy(
                            params = runRequest.params.copy(
                                mode = DiktatDemoMode.valueOf(event.target.value)
                            )
                        )
                    }
                }
            }
            div {
                className = ClassName("col-2 ml-1")
                selectorBuilder(
                    diktatRunRequest.params.tool.name,
                    DiktatDemoTool.values().map { it.name },
                    "custom-select"
                ) { event ->
                    setDiktatRunRequest { runRequest ->
                        runRequest.copy(
                            params = runRequest.params.copy(
                                tool = DiktatDemoTool.valueOf(event.target.value)
                            )
                        )
                    }
                }
            }
        }
        div {
            className = ClassName("mb-1 mt-1 d-flex justify-content-center")
            label {
                className = ClassName("btn btn-outline-secondary m-0")
                val reader = FileReader().apply {
                    onload = { event ->
                        setDiktatRunRequest { runRequest ->
                            runRequest.copy(
                                params = runRequest.params.copy(
                                    config = (event.target.asDynamic()["result"] as String?)
                                )
                            )
                        }
                    }
                }
                input {
                    type = InputType.file
                    multiple = false
                    hidden = true
                    onChange = { event ->
                        event.target.files!!.asList()
                            .firstOrNull()
                            ?.let { file ->
                                reader.readAsText(file, "UTF-8")
                            }
                    }
                }
                fontAwesomeIcon(icon = faUpload)
                val uploadOrReplace = if (diktatRunRequest.params.config.orEmpty()
                    .isEmpty()) {
                    "Upload"
                } else {
                    "Replace"
                }
                strong { +"$uploadOrReplace diktat-analysis.yml " }
            }
        }
        div {
            className = ClassName("mb-1 d-flex justify-content-center")
            buttonBuilder("Send run request") {
                sendRunRequest()
            }
        }
        div {
            className = ClassName("ml-1 mr-1")
            displayAlertWithWarnings(diktatResult.warnings) {
                setDiktatResult { result ->
                    result.copy(warnings = emptyList())
                }
            }
        }
    }
}
