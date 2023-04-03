/**
 * Function component for build window
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.demo.cpg.CpgAdditionalParams
import com.saveourtool.save.demo.cpg.CpgEngine
import com.saveourtool.save.demo.cpg.CpgRunRequest
import com.saveourtool.save.frontend.components.basic.codeeditor.codeEditorComponent
import com.saveourtool.save.frontend.components.basic.cpg.SigmaLayout
import com.saveourtool.save.frontend.externals.fontawesome.faCaretSquareRight
import com.saveourtool.save.frontend.externals.fontawesome.faInfoCircle
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.Languages

import csstype.ClassName
import csstype.Height
import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.useState

@Suppress(
    "TOO_LONG_FUNCTION",
    "KDOC_WITHOUT_RETURN_TAG",
    "LongMethod",
    "TYPE_ALIAS"
)
val demoComponent: FC<DemoComponentProps> = FC { props ->
    val (selectedLanguage, setSelectedLanguage) = useState(props.preselectedLanguage)
    val (codeLines, setCodeLines) = useState(props.placeholderText)
    val (selectedTheme, setSelectedTheme) = useState(AceThemes.preferredTheme)
    val (selectedEngine, setSelectedEngine) = useState(CpgEngine.CPG)

    val sendRunRequest = useDeferredRequest {
        props.resultRequest(
            this,
            CpgRunRequest(
                codeLines.split("\n"),
                CpgAdditionalParams(
                    engine = selectedEngine,
                    language = selectedLanguage,
                ),
            )
        )
    }

    div {
        className = ClassName("")
        div {
            className = ClassName("row")
            div {
                className = ClassName("col-4")
                codeEditorComponent {
                    editorTitle = "Input code"
                    this.selectedTheme = selectedTheme
                    selectedMode = selectedLanguage
                    savedText = codeLines
                    draftText = codeLines
                    onDraftTextUpdate = { code ->
                        setCodeLines(code)
                    }
                    isDisabled = false
                }
                useTooltip()
                div {
                    className = ClassName("card-body input-group pl-0 pr-0")
                    selectorBuilder(
                        props.selectedLayout.layoutName,
                        SigmaLayout.values().map { it.layoutName },
                        "form-control custom-select",
                    ) { event ->
                        props.setSelectedLayout(
                            SigmaLayout.values().find { it.layoutName == event.target.value }!!
                        )
                    }
                    selectorBuilder(
                        selectedTheme.themeName,
                        AceThemes.values().map { it.themeName },
                        "form-control custom-select",
                    ) { event ->
                        setSelectedTheme {
                            AceThemes.values().find { it.themeName == event.target.value }!!
                        }
                    }
                    selectorBuilder(
                        selectedLanguage.prettyName,
                        Languages.values().map { it.prettyName },
                        "form-control custom-select"
                    ) { event ->
                        setSelectedLanguage {
                            Languages.values().find { it.prettyName == event.target.value }!!
                        }
                    }
                    selectorBuilder(
                        selectedEngine.prettyName,
                        CpgEngine.values().map { it.prettyName },
                        "form-control custom-select"
                    ) { event ->
                        setSelectedEngine {
                            CpgEngine.values().find { it.prettyName == event.target.value }!!
                        }
                    }
                    div {
                        className = ClassName("input-group-append")
                        buttonBuilder(faCaretSquareRight, title = "Send run request", isOutline = true) {
                            sendRunRequest()
                        }
                        buttonBuilder(faInfoCircle, title = "Show logs", isOutline = true) {
                            props.changeLogsVisibility()
                        }
                    }
                }
            }

            div {
                className = ClassName("col-8 d-flex flex-wrap")
                div {
                    className = ClassName("col")
                    h6 {
                        className = ClassName("text-center flex-wrap text-primary")
                        +"Graph"
                    }
                    div {
                        className = ClassName("")
                        style = jso {
                            height = "100%".unsafeCast<Height>()
                        }
                        props.resultBuilder(this)
                    }
                }
            }
        }
    }
}

/**
 * DemoComponent [Props]
 */
@Suppress("TYPE_ALIAS")
external interface DemoComponentProps : Props {
    /**
     * Callback to display the result
     */
    var resultBuilder: (ChildrenBuilder) -> Unit

    /**
     * Request to receive the result
     */
    var resultRequest: suspend WithRequestStatusContext.(CpgRunRequest) -> Unit

    /**
     * Callback to display/hide the logs using Show logs button
     */
    var changeLogsVisibility: () -> Unit

    /**
     * Peace of code that is used to be put into "Input code" editor
     */
    var placeholderText: String

    /**
     * Language that will be preselected
     */
    var preselectedLanguage: Languages

    /**
     * Currently selected layout that should be applied in order to place nodes
     */
    var selectedLayout: SigmaLayout

    /**
     * Callback to update [selectedLayout]
     */
    var setSelectedLayout: (SigmaLayout) -> Unit
}
