/**
 * Function component for build window
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.demo.cpg.CpgAdditionalParams
import com.saveourtool.save.demo.cpg.CpgRunRequest
import com.saveourtool.save.frontend.components.basic.codeeditor.codeEditorComponent
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

    val sendRunRequest = useDeferredRequest {
        props.resultRequest(
            this,
            CpgRunRequest(
                codeLines.split("\n"),
                CpgAdditionalParams(language = selectedLanguage),
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

                div {
                    className = ClassName("card-body row d-flex justify-content-center")
                    div {
                        className = ClassName("mr-1")
                        selectorBuilder(
                            selectedTheme.themeName,
                            AceThemes.values().map { it.themeName },
                            "custom-select",
                        ) { event ->
                            setSelectedTheme {
                                AceThemes.values().find { it.themeName == event.target.value }!!
                            }
                        }
                    }
                    div {
                        className = ClassName("mr-1")
                        selectorBuilder(
                            selectedLanguage.prettyName,
                            Languages.values().map { it.prettyName },
                            "custom-select"
                        ) { event ->
                            setSelectedLanguage {
                                Languages.values().find { it.prettyName == event.target.value }!!
                            }
                        }
                    }
                    div {
                        buttonBuilder("Send run request", classes = "mr-1") {
                            sendRunRequest()
                        }
                    }
                    div {
                        buttonBuilder("Show logs") {
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
}
