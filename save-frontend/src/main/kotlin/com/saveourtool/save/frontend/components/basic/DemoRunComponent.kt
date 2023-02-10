/**
 * Function component for different demos
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.demo.DemoMode
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
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
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.strong
import web.file.FileReader
import web.html.InputType

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val demoRunComponent = demoRunComponent()

private val defaultCode = """
    |package com.test
    |
    |fun main() {
    |    val SAVEOUR = "tool"
    |}
""".trimMargin()

/**
 * DemoComponent [Props]
 */
external interface DemoRunComponentProps : Props {
    /**
     * Theme for Ace Editor
     */
    var selectedTheme: AceThemes

    /**
     * Mode for Ace Editor
     */
    var selectedMode: Languages

    /**
     * An initial value of [DemoRunRequest]
     */
    var emptyDemoRunRequest: DemoRunRequest

    /**
     * Endpoint to run this demo
     */
    var demoRunEndpoint: String

    /**
     * Optional config name for this demo
     */
    var configName: String?
}

private fun ChildrenBuilder.displayAlertWithWarnings(result: DemoResult, flushWarnings: () -> Unit) {
    div {
        val show = if (result.warnings.isEmpty() && result.logs.isEmpty()) {
            ""
        } else {
            "show"
        }
        val isError = result.terminationCode != 0 && result.warnings.isEmpty()
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
                +"Something went wrong... See the logs below:"
                result.logs.forEach { logLine ->
                    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
                    br { }
                    +logLine
                }
            }
        } else {
            h4 {
                className = ClassName("alert-heading")
                val warningWord = if (result.warnings.size == 1) {
                    "warning"
                } else {
                    "warnings"
                }
                +"Detected ${result.warnings.size} $warningWord:"
            }
            result.warnings.forEach { warning ->
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
private fun demoRunComponent() = FC<DemoRunComponentProps> { props ->
    val (demoRunRequest, setDemoRunRequest) = useState(props.emptyDemoRunRequest)
    val (diktatResult, setDiktatResult) = useState(DemoResult.empty)
    val (codeLines, setCodeLines) = useState(defaultCode)

    val sendRunRequest = useDeferredRequest {
        val result: DemoResult = post(
            "$demoApiUrl/${props.demoRunEndpoint}",
            jsonHeaders,
            Json.encodeToString(demoRunRequest.copy(
                codeLines = codeLines.split("\n"))
            ),
            ::loadingHandler,
            ::noopResponseHandler,
        )
            .decodeFromJsonString()
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
                    savedText = diktatResult.outputText.joinToString("\n")
                    draftText = diktatResult.outputText.joinToString("\n")
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
                    demoRunRequest.mode.toString(),
                    DemoMode.values().map { it.name },
                    "custom-select"
                ) { event ->
                    setDemoRunRequest { runRequest ->
                        runRequest.copy(
                            mode = DemoMode.valueOf(event.target.value)
                        )
                    }
                }
            }
        }
        props.configName?.let { configName ->
            div {
                className = ClassName("mb-1 mt-1 d-flex justify-content-center")
                label {
                    className = ClassName("btn btn-outline-secondary m-0")
                    val reader = FileReader().apply {
                        onload = { event ->
                            setDemoRunRequest { runRequest ->
                                (event.target.asDynamic()["result"] as String?)?.let {
                                    runRequest.copy(
                                        config = it.split("\n")
                                    )
                                } ?: runRequest
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
                    val uploadOrReplace = if (demoRunRequest.config.isNullOrEmpty()) {
                        "Upload"
                    } else {
                        "Replace"
                    }
                    strong { +"$uploadOrReplace $configName" }
                }
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
            displayAlertWithWarnings(diktatResult) {
                setDiktatResult(DemoResult.empty)
            }
        }
    }
}
