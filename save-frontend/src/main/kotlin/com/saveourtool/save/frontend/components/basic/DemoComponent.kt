/**
 * Function component for build window
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.demo.diktat.DemoAdditionalParams
import com.saveourtool.save.demo.diktat.DemoRunRequest
import com.saveourtool.save.frontend.components.basic.codeeditor.codeEditorComponent
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.Languages

import csstype.ClassName
import csstype.Display
import csstype.Height
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.useState

import kotlinx.js.jso

@Suppress(
    "TOO_LONG_FUNCTION",
    "KDOC_WITHOUT_RETURN_TAG",
    "LongMethod",
    "TYPE_ALIAS"
)
val demoComponent: FC<DemoComponentProps> = FC { props ->
    val (selectedLanguage, setSelectedLanguage) = useState(Languages.KOTLIN)
    val (codeLines, setCodeLines) = useState("")
    val (selectedTheme, setSelectedTheme) = useState(AceThemes.preferredTheme)

    val sendRunRequest = useDeferredRequest {
        props.resultRequest(
            this,
            DemoRunRequest(
                codeLines.split("\n"),
                DemoAdditionalParams(language = selectedLanguage),
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
                        buttonBuilder("Send run request") {
                            sendRunRequest()
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
                        className = ClassName("card card-body")
                        style = jso {
                            height = "90%".unsafeCast<Height>()
                            display = Display.block
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
    var resultRequest: suspend WithRequestStatusContext.(DemoRunRequest) -> Unit
}
