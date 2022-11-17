/**
 * Function component for build window
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.components.basic.codeeditor.codeEditorComponent
import com.saveourtool.save.frontend.externals.reactace.AceModes
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.*
import csstype.ClassName
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.useState

val builderComponent = builderComponent()

/**
 * DemoComponent [Props]
 */
@Suppress("TYPE_ALIAS")
external interface BuilderComponentProps : Props {
    /**
     * Theme for Ace Editor
     */
    var selectedTheme: AceThemes

    /**
     * Lambda for run request
     */
    var sendRunRequest: (String, AceModes) -> Unit
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "KDOC_WITHOUT_RETURN_TAG",
    "LongMethod",
    "TYPE_ALIAS"
)
private fun builderComponent() = FC<BuilderComponentProps> { props ->
    val (selectedLanguage, setSelectedLanguage) = useState(AceModes.KOTLIN)
    val (codeLines, setCodeLines) = useState("")

    div {
        className = ClassName("")
        div {
            className = ClassName("row")
            div {
                className = ClassName("col-4")
                codeEditorComponent {
                    editorTitle = "Input code"
                    selectedTheme = props.selectedTheme
                    selectedMode = selectedLanguage
                    savedText = codeLines
                    draftText = codeLines
                    onDraftTextUpdate = { code ->
                        setCodeLines(code)
                    }
                    isDisabled = false
                }
            }

            div {
                className = ClassName("col-8 card mt-4")
                // TODO: need to added window for graph
            }
        }
        div {
            className = ClassName("row mt-2 mb-1 d-flex justify-content-center")
            div {
                className = ClassName("col-2 mr-1")
                selectorBuilder(
                    selectedLanguage.prettyName,
                    AceModes.values().map { it.prettyName },
                    "custom-select"
                ) { event ->
                    setSelectedLanguage {
                        AceModes.values().find { it.prettyName == event.target.value }!!
                    }
                }
            }
        }

        div {
            className = ClassName("mb-1 d-flex justify-content-center")
            buttonBuilder("Send run request") {
                props.sendRunRequest(codeLines, selectedLanguage)
            }
        }
    }
}
