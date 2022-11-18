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
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.useState

val builderComponent = builderComponent()

/**
 * DemoComponent [Props]
 */
@Suppress("TYPE_ALIAS")
external interface BuilderComponentProps : Props {
    /**
     * Lambda for run request
     */
    var sendRunRequest: (String, AceModes) -> Unit

    /**
     * modal for builder window
     */
    var builderModal: (ChildrenBuilder) -> Unit
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
    val (selectedTheme, setSelectedTheme) = useState(AceThemes.preferredTheme)

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
                    className = ClassName("row d-flex justify-content-center")
                    div {
                        selectorBuilder(
                            selectedTheme.name,
                            AceThemes.values().map { it.name },
                            "custom-select",
                        ) { setSelectedTheme(AceThemes.valueOf(it.target.value)) }
                    }
                    div {
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
                    div {
                        buttonBuilder("Send run request") {
                            props.sendRunRequest(codeLines, selectedLanguage)
                        }
                    }
                }
            }

            div {
                className = ClassName("col-8")
                h6 {
                    className = ClassName("text-center text-primary")
                    +"Graph"
                }
                div {
                    className = ClassName("card card-body")
                }
                props.builderModal(this)
            }
        }
    }
}
